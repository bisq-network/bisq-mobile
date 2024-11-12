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
import bisq.common.observable.Observable
import bisq.user.identity.UserIdentityService
import bisq.application.State
import bisq.chat.ChatChannelDomain
import bisq.common.locale.LanguageRepository
import bisq.network.p2p.services.data.BroadcastResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.service.AndroidApplicationService
import java.util.Optional
import kotlin.jvm.optionals.getOrElse
import kotlin.random.Random

class MainNodePresenter(greetingRepository: GreetingRepository): MainPresenter(greetingRepository) {
    private val logMessage: Observable<String> = Observable("")
    val state = Observable(State.INITIALIZE_APP)
    private val shutDownErrorMessage = Observable<String>()
    private val startupErrorMessage = Observable<String>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val loggingScope = CoroutineScope(Dispatchers.IO)

    private lateinit var applicationService: AndroidApplicationService
    private lateinit var userIdentityService: UserIdentityService
//    private val userProfileController: UserProfileController

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
//            if (userIdentityService.userIdentities.isEmpty()) {
//                //createUserIfNoneExist();
//                userProfileController.initialize()
//
//                // mock profile creation and wait until done.
//                userProfileController.createUserProfile().join()
//                appendLog("Created profile for user", "")
//            }
//            printUserProfiles()

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