package network.bisq.mobile.android.node.presentation

import android.app.Activity
import android.os.Build
import android.os.Process
import bisq.application.State
import bisq.bonded_roles.market_price.MarketPrice
import bisq.chat.ChatChannelDomain
import bisq.chat.ChatMessageType
import bisq.chat.common.CommonPublicChatMessage
import bisq.chat.two_party.TwoPartyPrivateChatChannel
import bisq.chat.two_party.TwoPartyPrivateChatMessage
import bisq.common.currency.MarketRepository
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.locale.LanguageRepository
import bisq.common.network.AndroidEmulatorLocalhostFacade
import bisq.common.network.TransportType
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.timer.Scheduler
import bisq.common.util.MathUtils
import bisq.common.util.StringUtils
import bisq.network.p2p.node.Node
import bisq.network.p2p.services.data.BroadcastResult
import bisq.security.DigestUtil
import bisq.security.SecurityService
import bisq.user.identity.NymIdGenerator
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import kotlinx.coroutines.*
import network.bisq.mobile.android.node.AndroidNodeGreeting
import network.bisq.mobile.android.node.domain.data.repository.NodeGreetingRepository
import network.bisq.mobile.android.node.domain.model.UserProfileModel
import network.bisq.mobile.android.node.service.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.presentation.MainPresenter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.jvm.optionals.getOrElse
import kotlin.math.max
import kotlin.random.Random

@Suppress("UNCHECKED_CAST")
class MainNodePresenter(greetingRepository: NodeGreetingRepository): MainPresenter(greetingRepository as GreetingRepository<Greeting>) {
    companion object {
        private const val AVATAR_VERSION = 0
    }
    private val logMessage: Observable<String> = Observable("")
    val state = Observable(State.INITIALIZE_APP)
    private val shutDownErrorMessage = Observable<String>()
    private val startupErrorMessage = Observable<String>()

    private val profileModel = UserProfileModel()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val loggingScope = CoroutineScope(Dispatchers.IO)

    private lateinit var applicationService: AndroidApplicationService
    private lateinit var userIdentityService: UserIdentityService
    private lateinit var securityService: SecurityService


    init {
        // TODO move to application once DI setup gets merged
        FacadeProvider.setLocalhostFacade(AndroidEmulatorLocalhostFacade())
        FacadeProvider.setJdkFacade(AndroidJdkFacade(Process.myPid()))
        FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        log("Static Bisq core setup ready")

        CoroutineScope(Dispatchers.IO).launch {
            greetingRepository.create(AndroidNodeGreeting())
        }
    }
    override fun onViewAttached() {
        super.onViewAttached()
        logMessage.addObserver {
            println(it)
        }

        // TODO this should be injected to the presenter
        val context = (view as Activity).applicationContext
        val filesDirsPath = (view as Activity).filesDir.toPath()
        val androidMemoryService = AndroidMemoryReportService(context)
        log("Path for files dir $filesDirsPath")
        applicationService = AndroidApplicationService(androidMemoryService, filesDirsPath)
        userIdentityService = applicationService.userService.userIdentityService
        securityService = applicationService.securityService

        launchServices()
    }

    override fun onDestroying() {
        applicationService.shutdown()
        super.onDestroying()
    }

    private fun log(message: String) {
        loggingScope.launch {
            logMessage.set(message)
        }
    }

    private fun launchServices() {
        coroutineScope.launch {
            observeAppState()
            applicationService.readAllPersisted().join()
            applicationService.initialize().join()

            printDefaultKeyId()
            printLanguageCode()

            // At the moment is nor persisting the profile so it will create one on each run
            if (userIdentityService.userIdentities.isEmpty()) {
                //createUserIfNoneExist();
                initializeUserService()

                // mock profile creation and wait until done.
                createUserProfile("Android user " + Random(4234234).nextInt(100)).join()
                log("Created profile for user")
            }
            printUserProfiles()

            observeNetworkState()
            observeNumConnections()
            fetchMarketPrice(500L)

            observePrivateMessages()
            publishRandomChatMessage();
            observeChatMessages(5)
            maybeRemoveMyOldChatMessages()
            //
            sendRandomMessagesEvery(60L * 100L)
        }
    }


    ///// SETUP
    private fun printUserProfiles() {
        applicationService.userService.userIdentityService.userIdentities.stream()
            .map { obj: UserIdentity -> obj.userProfile }
            .map { userProfile: UserProfile -> userProfile.userName + " [" + userProfile.nym + "]" }
            .forEach { userName: String -> log("My profile $userName") }
    }

    private fun createUserProfile(nickName: String): CompletableFuture<UserIdentity> {
        // UI can listen to that state change and show busy animation
        profileModel.isBusy.set(true)
        return userIdentityService.createAndPublishNewUserProfile(
            nickName,
            profileModel.keyPair,
            profileModel.pubKeyHash,
            profileModel.proofOfWork,
            AVATAR_VERSION,
            profileModel.terms.get(),
            profileModel.statement.get()
        )
            .whenComplete { userIdentity: UserIdentity?, throwable: Throwable? ->
                // UI can listen to that state change and stop busy animation and show close button
                profileModel.isBusy.set(false)
            }
    }

    private fun initializeUserService() {
        val userIdentities = userIdentityService.userIdentities
        if (userIdentities.isEmpty()) {
            // Generate
            onGenerateKeyPair()
        } else {
            // If we have already a user profile we don't do anything. Leave it to the parent
            // controller to skip and not even create initialize controller.
            log("We have already a user profile.")
        }
    }

    private fun onGenerateKeyPair() {
        val keyPair = securityService.keyBundleService.generateKeyPair()
        profileModel.keyPair = keyPair
        val pubKeyHash = DigestUtil.hash(keyPair.public.encoded)
        profileModel.pubKeyHash = pubKeyHash
        val proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash)
        profileModel.proofOfWork = proofOfWork
        val powSolution = proofOfWork.solution
        val nym = NymIdGenerator.generate(pubKeyHash, powSolution)
        profileModel.nym.set(nym) // nym will be created on demand from pubKeyHash and pow
        // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
        //  Image image = CatHash.getImage(pubKeyHash,
        //                                powSolution,
        //                                CURRENT_AVATARS_VERSION,
        //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
    }

    private fun printDefaultKeyId() {
        log(
            "Default key ID: ${applicationService.securityService.keyBundleService.defaultKeyId}"
        )
    }

    private fun printLanguageCode() {
        log(
            "Language: ${LanguageRepository.getDisplayLanguage(applicationService.settingsService.languageCode.get())}"
        )
    }

    /////// USE CASES

    private fun observeAppState() {
        applicationService.state.addObserver { state: State ->
            log("Application state: ${state.name}")
        }
    }

    /**
     * This is key to bisq androidNode. Trading will happen through private messaging for us.
     */
    private fun observePrivateMessages() {
        val pinByChannelId: MutableMap<String, Pin> = HashMap()
        applicationService.chatService.twoPartyPrivateChatChannelService.channels
            .addObserver(object : CollectionObserver<TwoPartyPrivateChatChannel> {
                override fun add(channel: TwoPartyPrivateChatChannel) {
                    log("Private channel: ${channel.displayString}")
                    pinByChannelId.computeIfAbsent(
                        channel.id
                    ) { _: String? ->
                        channel.chatMessages.addObserver(object :
                            CollectionObserver<TwoPartyPrivateChatMessage> {
                            override fun add(message: TwoPartyPrivateChatMessage) {
                                if (message.chatMessageType == null) {
                                    return
                                }
                                var text = ""
                                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                                text = when (message.chatMessageType) {
                                    ChatMessageType.TEXT -> {
                                        message.text
                                    }

                                    ChatMessageType.LEAVE -> {
                                        "PEER LEFT " + message.text
                                        // leave handling not working yet correctly
                                        /* Scheduler.run(()->applicationService.chatService.getTwoPartyPrivateChatChannelService().leaveChannel(channel))
                                                        .after(500);*/
                                    }

                                    ChatMessageType.TAKE_BISQ_EASY_OFFER -> {
                                        "TAKE_BISQ_EASY_OFFER " + message.text
                                    }

                                    ChatMessageType.PROTOCOL_LOG_MESSAGE -> {
                                        "PROTOCOL_LOG_MESSAGE " + message.text
                                    }
                                }
                                val displayString = "[" + channel.displayString + "] " + text
                                log("Private message $displayString")
                            }

                            override fun remove(o: Any) {
                                // We do not support remove of PM
                            }

                            override fun clear() {
                            }
                        })
                    }
                }

                override fun remove(o: Any) {
                    if (o is TwoPartyPrivateChatChannel) {
                        val id: String = o.id
                        if (pinByChannelId.containsKey(id)) {
                            pinByChannelId[id]!!.unbind()
                            pinByChannelId.remove(id)
                        }
                        log("Closed private channel ${o.displayString}")
                    }
                }

                override fun clear() {
                }
            })
    }

    private fun observeNetworkState() {
        Optional.ofNullable(
            applicationService.networkService.defaultNodeStateByTransportType[TransportType.CLEAR]
        )
            .orElseGet { null }
            .addObserver { state: Node.State -> log("Network state: $state") }
    }

    private fun observeNumConnections() {
        val serviceNode =
            applicationService.networkService.serviceNodesByTransport.findServiceNode(TransportType.CLEAR)
                .orElseGet { null }
        val defaultNode = serviceNode?.defaultNode
        val peerGroupManager = serviceNode?.peerGroupManager?.orElseGet { null }
        val peerGroupService = peerGroupManager?.peerGroupService
        val numConnections = AtomicLong()
        Scheduler.run {
            val currentNumConnections = peerGroupService?.getAllConnectedPeers(defaultNode)?.count()
            if (numConnections.get() != currentNumConnections) {
                if (currentNumConnections != null) {
                    numConnections.set(currentNumConnections)
                }
                log("Number of connections: $currentNumConnections")
            }
        }.periodically(100)
    }

    /**
     * Fetch the market price, if not avail it will retry every delay ms.
     * TODO: change to fetch continously updating an observable that can be printed with each change
     */
    private fun fetchMarketPrice(retryDelay: Long) {
        val priceQuote = applicationService.bondedRolesService.marketPriceService
            .findMarketPrice(MarketRepository.getUSDBitcoinMarket())
            .map { e: MarketPrice ->
                MathUtils.roundDouble(e.priceQuote.value / 10000.0, 2).toString() + " BTC/USD"
            }
        if (!priceQuote.isPresent) {
            Scheduler.run { this.fetchMarketPrice(retryDelay) }.after(retryDelay)
        } else {
            log("Market price: ${priceQuote.get()}")
        }
    }

    private fun sendRandomMessagesEvery(delayMs: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) { // Coroutine will keep running while active
                publishRandomChatMessage()
                delay(delayMs) // Delay for 1 minute (60,000 ms)
            }
        }
    }

    private fun publishRandomChatMessage() {
        val userService = applicationService.userService
        val userIdentityService = userService.userIdentityService
        val chatService = applicationService.chatService
        val chatChannelDomain = ChatChannelDomain.DISCUSSION
        val discussionChannelService =
            chatService.commonPublicChatChannelServices[chatChannelDomain]
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            discussionChannelService!!.channels.stream().findFirst().orElseThrow()
        } else {
            discussionChannelService!!.channels.stream().findFirst().getOrElse { null }
        }
        val userIdentity = userIdentityService.selectedUserIdentity
        discussionChannelService.publishChatMessage(
            "Dev message " + Random(34532454325).nextInt(100),
            Optional.empty(),
            channel,
            userIdentity
        ).whenComplete { result: BroadcastResult?, _: Throwable? ->
            log("publishChatMessage result $result")
        }
    }

    private fun observeChatMessages(numLastMessages: Int) {
        val userService = applicationService.userService
        val chatService = applicationService.chatService
        val chatChannelDomain = ChatChannelDomain.DISCUSSION
        val discussionChannelService =
            chatService.commonPublicChatChannelServices[chatChannelDomain]
        val channel = discussionChannelService!!.channels.stream().findFirst().orElseGet { null }
        val toSkip = max(0.0, (channel.chatMessages.size - numLastMessages).toDouble())
            .toInt()
        val displayedMessages: MutableList<String> = ArrayList()
        channel.chatMessages.stream()
            .sorted(Comparator.comparingLong { obj: CommonPublicChatMessage -> obj.date })
            .map { message: CommonPublicChatMessage ->
                displayedMessages.add(message.id)
                val authorUserProfileId = message.authorUserProfileId
                val userName = userService.userProfileService.findUserProfile(authorUserProfileId)
                    .map { obj: UserProfile -> obj.userName }
                    .orElse("N/A")
                maybeRemoveMyOldChatMessages()
                "{" + userName + "} " + message.text
            }
            .skip(toSkip.toLong())
            .forEach { e: String -> log("Chat message $e") }

        channel.chatMessages.addObserver(object : CollectionObserver<CommonPublicChatMessage> {
            override fun add(message: CommonPublicChatMessage) {
                if (displayedMessages.contains(message.id)) {
                    return
                }
                displayedMessages.add(message.id)
                val authorUserProfileId = message.authorUserProfileId
                val userName = userService.userProfileService.findUserProfile(authorUserProfileId)
                    .map { obj: UserProfile -> obj.userName }
                    .orElse("N/A")
                val text = message.text
                val displayString = "{$userName} $text"
                log("Chat message: $displayString")
                maybeRemoveMyOldChatMessages()
            }

            override fun remove(o: Any) {
                if (o is CommonPublicChatMessage) log("Removed chat message: ${o.text}")
            }

            override fun clear() {
            }
        })
    }

    private fun maybeRemoveMyOldChatMessages() {
        val userService = applicationService.userService
        val userIdentityService = userService.userIdentityService
        val userIdentity = userIdentityService.selectedUserIdentity
        val chatService = applicationService.chatService
        val chatChannelDomain = ChatChannelDomain.DISCUSSION
        val discussionChannelService =
            chatService.commonPublicChatChannelServices[chatChannelDomain]
        val channel = discussionChannelService!!.channels.stream().findFirst().orElseGet { null }
        val myProfileId = userIdentity.userProfile.id
        log("Number of chat messages: ${channel.chatMessages.size}")
        val expireDate = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30)
        channel.chatMessages.stream()
            .filter { message: CommonPublicChatMessage -> message.date < expireDate }
            .filter { message: CommonPublicChatMessage -> myProfileId == message.authorUserProfileId }
            .forEach { message: CommonPublicChatMessage ->
                log("Remove my old chat message: ${StringUtils.truncate(message.text, 10)}")
                discussionChannelService.deleteChatMessage(
                    message,
                    userIdentity.networkIdWithKeyPair
                ).whenComplete { r: BroadcastResult, t: Throwable? ->
                    log("Remove message result: ${t == null}")
                    log("Error: $r")
                }
            }
    }
}