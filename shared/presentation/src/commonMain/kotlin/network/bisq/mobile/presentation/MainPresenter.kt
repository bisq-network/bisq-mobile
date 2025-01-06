package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.getDeviceLanguageCode
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.presentation.ui.AppPresenter
import kotlin.random.Random


/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter(
    private val notificationServiceController: NotificationServiceController,
    private val urlLauncher: UrlLauncher
) :
    BasePresenter(null), AppPresenter {
    companion object {
        // FIXME this will be erased eventually, for now you can turn on to see the notifications working
        // it will push a notification every 60 sec
        const val testNotifications = false
        const val PUSH_DELAY = 60000L
    }

    override lateinit var navController: NavHostController
    override lateinit var tabNavController: NavHostController

    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible

    init {
        val localeCode = getDeviceLanguageCode()
        log.i { "Shared Version: ${BuildConfig.SHARED_LIBS_VERSION}" }
        log.i { "iOS Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Node Version: ${BuildNodeConfig.APP_VERSION}" }
        log.i { "Device language code: $localeCode"}
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

    override fun getRootNavController(): NavHostController {
        return navController
    }

    override fun getRootTabNavController(): NavHostController {
        return tabNavController
    }

    final override fun navigateToUrl(url: String) {
        urlLauncher.openUrl(url)
    }

}