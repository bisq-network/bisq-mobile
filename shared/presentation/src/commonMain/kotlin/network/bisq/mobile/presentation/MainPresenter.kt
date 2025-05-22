package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.model.TradeReadState
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.getDeviceLanguageCode
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.BisqConfig
import network.bisq.mobile.presentation.ui.navigation.Routes

/**
 * Main Presenter as an example of implementation for now.
 */
open class MainPresenter(
    private val connectivityService: ConnectivityService,
    private val openTradesNotificationService: OpenTradesNotificationService,
    private val settingsService: SettingsServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val urlLauncher: UrlLauncher
) : BasePresenter(null), AppPresenter {

    override lateinit var navController: NavHostController
    override lateinit var tabNavController: NavHostController

    // Observable state
    private val _isContentVisible = MutableStateFlow(false)
    override val isContentVisible: StateFlow<Boolean> = _isContentVisible

    private val _isSmallScreen = MutableStateFlow(false)
    override val isSmallScreen: StateFlow<Boolean> = _isSmallScreen

    final override val languageCode: StateFlow<String> = settingsService.languageCode

    private val _tradesWithUnreadMessages: MutableStateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> = _tradesWithUnreadMessages

    private val _readMessageCountsByTrade = MutableStateFlow(emptyMap<String, Int>())
    override val readMessageCountsByTrade: StateFlow<Map<String, Int>> = _readMessageCountsByTrade

    init {
        val localeCode = getDeviceLanguageCode()
        val screenWidth = getScreenWidthDp()
        _isSmallScreen.value = screenWidth < 480
        log.i { "Shared Version: ${BuildConfig.SHARED_LIBS_VERSION}" }
        log.i { "iOS Client Version: ${BuildConfig.IOS_APP_VERSION}" }
        log.i { "Android Client Version: ${BuildConfig.ANDROID_APP_VERSION}" }
        log.i { "Android Node Version: ${BuildNodeConfig.APP_VERSION}" }
        log.i { "Device language code: $localeCode" }
        log.i { "Screen width: $screenWidth" }
        log.i { "Small screen: ${_isSmallScreen.value}" }

        val notificationTimerFlow = flow {
            while (true) {
                emit(Unit)
                delay(BisqConfig.NOTIFICATION_SYNC_INTERVAL)
            }
        }

        launchIO {
            combine(
                tradesServiceFacade.openTradeItems,
                tradesServiceFacade.selectedTrade,
                notificationTimerFlow.onStart { emit(Unit) }
            ) { tradeList, selectedTrade, _ ->
                tradeList
            }.collect {
                val readState = tradeReadStateRepository.fetch() ?: TradeReadState()
                _readMessageCountsByTrade.value = readState.map
                _tradesWithUnreadMessages.value = it.map { trade ->
                    val chatSize = trade.bisqEasyOpenTradeChannelModel.chatMessages.value.size
                    return@map trade.tradeId to chatSize
                }.filter { idSizePair ->
                    val recordedSize = readState.map[idSizePair.first]
                    if (recordedSize != null && recordedSize >= idSizePair.second) {
                        return@filter false
                    }
                    return@filter true
                }.toMap()
            }
        }
    }

    @CallSuper
    override fun onViewAttached() {
        super.onViewAttached()

        languageCode
            .filter { it.isNotEmpty() }
            .onEach {
                // I18nSupport.initialize(it) // Done in App.kt, before view initializes
                setDefaultLocale(it)
                settingsService.setLanguageCode(it)
            }
            .take(1)
            .launchIn(presenterScope)

    }

    override fun onResume() {
        super.onResume()
        onResumeServices()
    }

    override fun onPause() {
        onPauseServices()
        super.onPause()
    }

    @CallSuper
    override fun onDestroying() {
        // to stop notification service and fully kill app (no zombie mode)
        stopOpenTradeNotificationsService()
        super.onDestroying()
    }

    open fun reactivateServices() {
        // default do nth
    }

    protected open fun onResumeServices() {
        stopOpenTradeNotificationsService()
        connectivityService.startMonitoring()
    }

    protected open fun onPauseServices() {
        connectivityService.stopMonitoring()
        openTradesNotificationService.launchNotificationService()
    }

    private fun stopOpenTradeNotificationsService() {
        openTradesNotificationService.stopNotificationService()
    }

    // Toggle action
    override fun toggleContentVisibility() {
        _isContentVisible.value = !_isContentVisible.value
    }

    override fun navigateToTrustedNode() {
        tabNavController.navigate(Routes.TabSettings.name)
        navController.navigate(Routes.TrustedNodeSettings.name)
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

    override fun isDemo(): Boolean = false

}