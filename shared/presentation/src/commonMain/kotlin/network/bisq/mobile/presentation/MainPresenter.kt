package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.service.trades.ClientTradesServiceFacade
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.getDeviceLanguageCode
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.setupUncaughtExceptionHandler
import network.bisq.mobile.presentation.ui.AppPresenter
import kotlin.jvm.JvmStatic
import kotlin.random.Random


/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter(
    protected val tradesServiceFacade: TradesServiceFacade,
    private val notificationServiceController: NotificationServiceController,
    private val urlLauncher: UrlLauncher
) : BasePresenter(null), AppPresenter {
    companion object {
        // TODO based on this flag show user a modal explaining internal crash, devs reporte,d with a button to quit the app
        val _systemCrashed = MutableStateFlow(false)

        @JvmStatic
        fun init() {
            setupUncaughtExceptionHandler({
                _systemCrashed.value = true
            })
        }
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
        notificationServiceController.registerObserver(tradesServiceFacade.openTradeItems) { newValue ->
            log.d { "open trades in total: ${newValue.size}" }
            newValue.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }.forEach { trade ->
                log.d { "open trade: $trade" }
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

    public final override fun pushNotification(title: String, content: String) {
        val randomTitle = "Title $title ${Random.nextInt(1, 100)}"
        val randomMessage = "Message $content ${Random.nextInt(1, 100)}"
        notificationServiceController.pushNotification(randomTitle, randomMessage)
        log.d {"Pushed: $randomTitle - $randomMessage" }
    }

    final override fun navigateToUrl(url: String) {
        urlLauncher.openUrl(url)
    }

}