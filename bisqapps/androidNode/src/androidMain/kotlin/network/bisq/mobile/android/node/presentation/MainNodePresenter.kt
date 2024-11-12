package network.bisq.mobile.android.node.presentation

import android.app.Activity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.service.AndroidApplicationService

class MainNodePresenter(greetingRepository: GreetingRepository): MainPresenter(greetingRepository) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(MainNodePresenter::class.java)
    }


    private val logMessage: Observable<String> = Observable("")
    val state = Observable(State.INITIALIZE_APP)
    private val shutDownErrorMessage = Observable<String>()
    private val startupErrorMessage = Observable<String>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
        log.debug("Static Bisq core setup ready")

    }
    override fun onViewAttached() {
        super.onViewAttached()
        logMessage.addObserver {
            log.debug("\n $it")
        }
        val filesDirsPath = (view as Activity).filesDir.toPath()
        logMessage.set("Path for files dir $filesDirsPath")

        // TODO this should be injected to the presenter
        applicationService = AndroidApplicationService(filesDirsPath)
        userIdentityService = applicationService.userService.userIdentityService
        launchServices()
    }

    private fun launchServices() {
        coroutineScope.launch {
//            observeAppState()
            applicationService.readAllPersisted().join()
            applicationService.initialize().join()

//            printDefaultKeyId()
//            printLanguageCode()

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

//        view.initialize()
    }
}