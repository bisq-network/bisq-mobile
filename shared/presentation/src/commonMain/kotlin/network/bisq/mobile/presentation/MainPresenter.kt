package network.bisq.mobile.presentation

import androidx.annotation.CallSuper
import androidx.navigation.NavHostController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.getDeviceLanguageCode
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
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
    override val isContentVisible: StateFlow<Boolean> get() = _isContentVisible.asStateFlow()

    private val _isSmallScreen = MutableStateFlow(false)
    override val isSmallScreen: StateFlow<Boolean> get() = _isSmallScreen.asStateFlow()

    final override val languageCode: StateFlow<String> get() = settingsService.languageCode

    // TODO: refactor when TradeItemPresentationModel is completely immutable
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> =
        tradesServiceFacade.openTradeItems
            .map { openTradeItems ->
                // For each trade, create a flow for its chatMessages count AND a flow for its trade state
                val messageFlows = openTradeItems.map { trade ->
                    trade.bisqEasyOpenTradeChannelModel.chatMessages
                        .map { messages -> trade.tradeId to messages.size }
                }
                val stateFlows = openTradeItems.map { trade ->
                    trade.bisqEasyTradeModel.tradeState
                        .map { state -> trade.tradeId to state }
                }
                messageFlows to stateFlows
            }
            .flatMapLatest { (messageFlows, stateFlows) ->
                // Combine all chatMessages flows and all tradeState flows into single emissions
                combine(
                    combine(messageFlows) { pairs -> pairs.toList() },
                    combine(stateFlows) { pairs -> pairs.toList() }
                ) { messagePairs, statePairs ->
                    messagePairs to statePairs
                }
            }
            .combine(tradeReadStateRepository.data.map { it.map }) { (tradeMessageCounts, tradeStates), tradeReadStates ->
                val messageMap = tradeMessageCounts.associate { it }
                val stateMap = tradeStates.associate { it }
                messageMap.filter { (tradeId, messageCount) ->
                    val isFinal = stateMap[tradeId]?.isFinalState == true
                    if (isFinal) return@filter false
                    val readCount = tradeReadStates.getOrElse(tradeId) { 0 }
                    readCount < messageCount
                }
            }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                emptyMap(),
            )

    override val showAnimation: StateFlow<Boolean> get() = settingsService.useAnimations

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
    }


    @CallSuper
    override fun onViewAttached() {
        super.onViewAttached()

        languageCode.filter { it.isNotEmpty() }.onEach {
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

    /**
     * Common error handling method for initialization and service activation errors.
     * Provides user-friendly error messages with contextual guidance.
     *
     * @param exception The exception that occurred
     * @param context Additional context about where the error occurred (e.g., "Node initialization", "Service activation")
     */
    protected fun handleInitializationError(
        exception: Throwable,
        context: String = "Initialization"
    ) {
        // Use the existing error handling infrastructure
        launchUI {
            GenericErrorHandler.handleGenericError(
                "Initialization process failed during: $context",
                exception
            )
        }
    }

}
