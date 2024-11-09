package bisq.android.main

import android.os.Process
import bisq.android.AndroidApplicationService
import bisq.android.main.user_profile.UserProfileController
import bisq.application.State
import bisq.bonded_roles.market_price.MarketPrice
import bisq.chat.ChatChannelDomain
import bisq.chat.ChatMessageType
import bisq.chat.common.CommonPublicChatMessage
import bisq.chat.two_party.TwoPartyPrivateChatChannel
import bisq.chat.two_party.TwoPartyPrivateChatMessage
import bisq.common.currency.MarketRepository
import bisq.common.encoding.Hex
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
import bisq.i18n.Res
import bisq.network.p2p.node.Node
import bisq.network.p2p.services.data.BroadcastResult
import bisq.security.DigestUtil
import bisq.user.identity.NymIdGenerator
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import com.google.common.base.Joiner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lombok.extern.slf4j.Slf4j
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.Security
import java.util.Optional
import java.util.Random
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@Slf4j
class MainController(userDataDir: Path?) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(MainController::class.java)
    }

    private val model: MainModel
    private val view: MainView
    private val applicationService: AndroidApplicationService
    private val userProfileController: UserProfileController
    private val userIdentityService: UserIdentityService

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        FacadeProvider.setLocalhostFacade(AndroidEmulatorLocalhostFacade())
        FacadeProvider.setJdkFacade(AndroidJdkFacade(Process.myPid()))
        FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        applicationService = AndroidApplicationService(userDataDir)
        model = MainModel()
        view = MainView(this, model)

        userIdentityService = applicationService.userService.userIdentityService

        userProfileController = UserProfileController(
            applicationService.userService,
            applicationService.securityService
        )
    }

    fun initialize() {
        coroutineScope.launch {
            observeAppState()
            applicationService.readAllPersisted().join()
            applicationService.initialize().join()

            printDefaultKeyId()
            printLanguageCode()

            // At the moment is nor persisting the profile so it will create one on each run
            if (userIdentityService.userIdentities.isEmpty()) {
                //createUserIfNoneExist();
                userProfileController.initialize()

                // mock profile creation and wait until done.
                userProfileController.createUserProfile().join()
                appendLog("Created profile for user", "")
            }
            printUserProfiles()

            observeNetworkState() // prints to screen
            observeNumConnections()
            printMarketPrice()

            observePrivateMessages()
            // publishRandomChatMessage();
            observeChatMessages(5)
            maybeRemoveMyOldChatMessages()

            sendRandomMessagesEvery(60 * 100)
        }

        view.initialize()
    }

    private fun sendRandomMessagesEvery(delayMs: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) { // Coroutine will keep running while active
                publishRandomChatMessage()
                delay(delayMs) // Delay for 1 minute (60,000 ms)
            }
        }
    }

    val logMessage: Observable<String>
        get() = model.logMessage

    // Use cases
    private fun observeNetworkState() {
        Optional.ofNullable(
            applicationService.networkService.defaultNodeStateByTransportType[TransportType.CLEAR]
        )
            .orElseThrow()
            .addObserver { state: Node.State -> appendLog("Network state", state) }
    }

    private fun observeNumConnections() {
        val serviceNode =
            applicationService.networkService.serviceNodesByTransport.findServiceNode(TransportType.CLEAR)
                .orElseThrow()
        val defaultNode = serviceNode.defaultNode
        val peerGroupManager = serviceNode.peerGroupManager.orElseThrow()
        val peerGroupService = peerGroupManager.peerGroupService
        val numConnections = AtomicLong()
        Scheduler.run {
            val currentNumConnections = peerGroupService.getAllConnectedPeers(defaultNode).count()
            if (numConnections.get() != currentNumConnections) {
                numConnections.set(currentNumConnections)
                appendLog("Number of connections", currentNumConnections)
            }
        }.periodically(100)
    }

    private fun observePrivateMessages() {
        val pinByChannelId: MutableMap<String, Pin> = HashMap()
        applicationService.chatService.twoPartyPrivateChatChannelService.channels
            .addObserver(object : CollectionObserver<TwoPartyPrivateChatChannel> {
                override fun add(channel: TwoPartyPrivateChatChannel) {
                    appendLog("Private channel", channel.displayString)
                    pinByChannelId.computeIfAbsent(
                        channel.id
                    ) { _: String? ->
                        channel.chatMessages.addObserver(object :
                            CollectionObserver<TwoPartyPrivateChatMessage> {
                            override fun add(message: TwoPartyPrivateChatMessage) {
                                var text = ""
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
                                appendLog("Private message", displayString)
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
                        appendLog("Closed private channel", o.displayString)
                    }
                }

                override fun clear() {
                }
            })
    }

    private fun observeAppState() {
        applicationService.state.addObserver { state: State ->
            appendLog(
                "Application state",
                Res.get("splash.applicationServiceState." + state.name)
            )
        }
    }

    private fun printDefaultKeyId() {
        appendLog(
            "Default key ID",
            applicationService.securityService.keyBundleService.defaultKeyId
        )
    }

    private fun printLanguageCode() {
        appendLog(
            "Language",
            LanguageRepository.getDisplayLanguage(applicationService.settingsService.languageCode.get())
        )
    }

    private fun printMarketPrice() {
        val priceQuote = applicationService.bondedRolesService.marketPriceService
            .findMarketPrice(MarketRepository.getUSDBitcoinMarket())
            .map { e: MarketPrice ->
                MathUtils.roundDouble(e.priceQuote.value / 10000.0, 2).toString() + " BTC/USD"
            }
        if (priceQuote.isEmpty) {
            Scheduler.run { this.printMarketPrice() }.after(500)
        } else {
            appendLog("Market price", priceQuote.get())
        }
    }

    private fun createUserIfNoneExist() {
        val userIdentityService = applicationService.userService.userIdentityService
        val userIdentities = userIdentityService.userIdentities
        if (userIdentities.isEmpty()) {
            val nickName = "Android " + Random().nextInt(100)
            val keyPair = applicationService.securityService.keyBundleService.generateKeyPair()
            val pubKeyHash = DigestUtil.hash(keyPair.public.encoded)
            val proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash)
            val powSolution = proofOfWork.solution
            val nym = NymIdGenerator.generate(
                pubKeyHash,
                powSolution
            ) // nym will be created on demand from pubKeyHash and pow
            // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
            //  Image image = CatHash.getImage(pubKeyHash,
            //                                powSolution,
            //                                CURRENT_AVATARS_VERSION,
            //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
            val avatarVersion = 0
            val terms = ""
            val statement = ""
            appendLog("Create new user with", "")
            appendLog("nickName", nickName)
            appendLog("pubKeyHash", Hex.encode(pubKeyHash))
            appendLog("nym", nym)
            userIdentityService.createAndPublishNewUserProfile(
                nickName,
                keyPair,
                pubKeyHash,
                proofOfWork,
                avatarVersion,
                terms,
                statement
            ).join()
        }
    }

    private fun printUserProfiles() {
        applicationService.userService.userIdentityService.userIdentities.stream()
            .map { obj: UserIdentity -> obj.userProfile }
            .map { userProfile: UserProfile -> userProfile.userName + " [" + userProfile.nym + "]" }
            .forEach { userName: String -> appendLog("My profile", userName) }
    }

    private fun publishRandomChatMessage() {
        val userService = applicationService.userService
        val userIdentityService = userService.userIdentityService
        val chatService = applicationService.chatService
        val chatChannelDomain = ChatChannelDomain.DISCUSSION
        val discussionChannelService =
            chatService.commonPublicChatChannelServices[chatChannelDomain]
        val channel = discussionChannelService!!.channels.stream().findFirst().orElseThrow()
        val userIdentity = userIdentityService.selectedUserIdentity
        discussionChannelService.publishChatMessage(
            "Dev message " + Random().nextInt(100),
            Optional.empty(),
            channel,
            userIdentity
        )
            .whenComplete { result: BroadcastResult?, _: Throwable? ->
                log.info("publishChatMessage result {}", result)
            }
    }

    private fun observeChatMessages(numLastMessages: Int) {
        val userService = applicationService.userService
        val chatService = applicationService.chatService
        val chatChannelDomain = ChatChannelDomain.DISCUSSION
        val discussionChannelService =
            chatService.commonPublicChatChannelServices[chatChannelDomain]
        val channel = discussionChannelService!!.channels.stream().findFirst().orElseThrow()
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
            .forEach { e: String -> appendLog("Chat message", e) }

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
                appendLog("Chat message", displayString)
                maybeRemoveMyOldChatMessages()
            }

            override fun remove(o: Any) {
                if (o is CommonPublicChatMessage) appendLog("Removed chat message", o.text)
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
        val channel = discussionChannelService!!.channels.stream().findFirst().orElseThrow()
        val myProfileId = userIdentity.userProfile.id
        appendLog("Number of chat messages", channel.chatMessages.size)
        val expireDate = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30)
        channel.chatMessages.stream()
            .filter { message: CommonPublicChatMessage -> message.date < expireDate }
            .filter { message: CommonPublicChatMessage -> myProfileId == message.authorUserProfileId }
            .forEach { message: CommonPublicChatMessage ->
                appendLog("Remove my old chat message", StringUtils.truncate(message.text, 5))
                discussionChannelService.deleteChatMessage(
                    message,
                    userIdentity.networkIdWithKeyPair
                )
                    .whenComplete { r: BroadcastResult, t: Throwable? ->
                        appendLog("Remove message result", t == null)
                        log.error(r.toString())
                    }
            }
    }

    private fun appendLog(key: String, value: Any) {
        val line = "$key: $value"
        val logMessages = model.logMessages
        logMessages.add(line)
        if (logMessages.size > 20) {
            logMessages.removeAt(0)
        }
        model.logMessage.set(Joiner.on("\n").join(logMessages))
    }
}
