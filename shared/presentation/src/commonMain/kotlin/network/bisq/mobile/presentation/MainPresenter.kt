package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
import network.bisq.mobile.presentation.ui.navigation.Routes

/**
 * Main Presenter as an example of implementation for now.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

        observeNotifications()
    }


    @CallSuper
    override fun onViewAttached() {
        super.onViewAttached()

        languageCode.filter { it.isNotEmpty() }.onEach {
            // I18nSupport.initialize(it) // Done in App.kt, before view initializes
            setDefaultLocale(it)
            settingsService.setLanguageCode(it)
        }.take(1).launchIn(presenterScope)

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

    fun isConnected(): Boolean {
        return connectivityService.isConnected()
    }

    open fun reactivateServices() {
        log.d { "Reactivating services default: skip" }
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

    private fun observeNotifications() {
        launchIO {
            combine(
                tradesServiceFacade.openTradeItems, tradesServiceFacade.selectedTrade
            ) { tradeList, selectedTrade ->
                // Combine all chatMessages StateFlows from each trade
                val combinedChatMessages = if (tradeList.isNotEmpty()) {
                    combine(tradeList.map { trade ->
                        trade.bisqEasyOpenTradeChannelModel.chatMessages.map { messages ->
                            trade.tradeId to messages.size
                        }
                    }) { tradeIdSizePairs ->
                        tradeIdSizePairs.toMap()
                    }
                } else {
                    flowOf(emptyMap<String, Int>())
                }

                combinedChatMessages.map { chatSizes ->
                    Triple(tradeList, chatSizes, selectedTrade)
                }
            }.flatMapLatest { it }.collect { (tradeList, chatSizes, _) ->
                val readState = tradeReadStateRepository.fetch() ?: TradeReadState()
                _readMessageCountsByTrade.value = readState.map
                _tradesWithUnreadMessages.value = tradeList.associate { trade ->
                    val chatSize = chatSizes[trade.tradeId] ?: 0
                    trade.tradeId to chatSize
                }.filter { (tradeId, chatSize) ->
                    val recordedSize = readState.map[tradeId]
                    recordedSize == null || recordedSize < chatSize
                }

            }

        }
    }

}