package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.presentation.ui.AppPresenter
import kotlin.random.Random


/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter(private val notificationServiceController: NotificationServiceController) :
    BasePresenter(null), AppPresenter {
    companion object {
        // FIXME this will be erased eventually, for now you can turn on to see the notifications working
        // it will push a notification every 60 sec
        const val testNotifications = false
        const val PUSH_DELAY = 60000L
    }

    lateinit var navController: NavHostController
        private set

    override fun setNavController(controller: NavHostController) {
        navController = controller
    }

    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible


    // passthrough example
    //    private val _greetingText: StateFlow<String> = stateFlowFromRepository(
    //        repositoryFlow = greetingRepository.data,
    //        transform = { it?.greet() ?: "" },
    //        initialValue = "Welcome!"
    //    )
    //    override val greetingText: StateFlow<String> = _greetingText

    init {
        log.i { "Shared Version: ${BuildConfig.SHARED_LIBS_VERSION}" }
        log.i { "iOS Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Node Version: ${BuildNodeConfig.APP_VERSION}" }
    }

    @CallSuper
    override fun onViewAttached() {
        super.onViewAttached()
        notificationServiceController.startService()
        // sample code for push notifications sends a random message every 10 secs
        if (testNotifications) {
            backgroundScope.launch {
                while (notificationServiceController.isServiceRunning()) {
                    val randomTitle = "Title ${Random.nextInt(1, 100)}"
                    val randomMessage = "Message ${Random.nextInt(1, 100)}"
                    notificationServiceController.pushNotification(randomTitle, randomMessage)
                    log.d {"Pushed: $randomTitle - $randomMessage" }
                    delay(PUSH_DELAY) // 1 min
                }
            }
        }
    }

    // Toggle action
    override fun toggleContentVisibility() {
        _isContentVisible.value = !_isContentVisible.value
    }

    override fun isIOS(): Boolean {
        val platformInfo = getPlatformInfo()
        val isIOS = platformInfo.name.lowercase().contains("ios")
        return isIOS
    }

}