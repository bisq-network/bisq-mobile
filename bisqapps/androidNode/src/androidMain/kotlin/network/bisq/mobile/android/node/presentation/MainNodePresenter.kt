package network.bisq.mobile.android.node.presentation

import android.app.Activity
import android.os.Build
import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.presentation.MainPresenter
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.network.AndroidEmulatorLocalhostFacade
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.Security
import android.os.Process
import bisq.android.main.user_profile.UserProfileModel
import bisq.common.observable.Observable
import bisq.user.identity.UserIdentityService
import bisq.application.State
import bisq.chat.ChatChannelDomain
import bisq.common.locale.LanguageRepository
import bisq.network.p2p.services.data.BroadcastResult
import bisq.security.DigestUtil
import bisq.security.SecurityService
import bisq.user.identity.NymIdGenerator
import bisq.user.identity.UserIdentity
import bisq.user.profile.UserProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.service.AndroidApplicationService
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrElse
import kotlin.random.Random

class MainNodePresenter(greetingRepository: GreetingRepository): MainPresenter(greetingRepository) {
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
    }
    override fun onViewAttached() {
        super.onViewAttached()
        logMessage.addObserver {
            println(it)
        }

        // TODO this should be injected to the presenter
        val filesDirsPath = (view as Activity).filesDir.toPath()
        log("Path for files dir $filesDirsPath")
        applicationService = AndroidApplicationService(filesDirsPath)
        userIdentityService = applicationService.userService.userIdentityService
        securityService = applicationService.securityService

        launchServices()
    }

    private fun log(message: String) {
        loggingScope.launch {
            logMessage.set(message)
        }
    }

    private fun launchServices() {
        coroutineScope.launch {
//            observeAppState()
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
                logMessage.set("Created profile for user")
            }
            printUserProfiles()

//            observeNetworkState() // prints to screen
//            observeNumConnections()
//            printMarketPrice()
//
//            observePrivateMessages()
//            // publishRandomChatMessage();
//            observeChatMessages(5)
//            maybeRemoveMyOldChatMessages()
//
//            sendRandomMessagesEvery(60 * 100)
        }
    }

    private fun printUserProfiles() {
        applicationService.userService.userIdentityService.userIdentities.stream()
            .map { obj: UserIdentity -> obj.userProfile }
            .map { userProfile: UserProfile -> userProfile.userName + " [" + userProfile.nym + "]" }
            .forEach { userName: String -> logMessage.set("My profile $userName") }
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
            logMessage.set("We have already a user profile.")
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
}