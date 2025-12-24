package network.bisq.mobile.presentation.tabs.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

open class DashboardPresenter(
    private val mainPresenter: MainPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val notificationController: NotificationController,
    private val foregroundDetector: ForegroundDetector,
    val platformSettingsManager: PlatformSettingsManager,
) : BasePresenter(mainPresenter) {
    private val _offersOnline = MutableStateFlow(0)
    val offersOnline: StateFlow<Int> get() = _offersOnline.asStateFlow()

    private val _publishedProfiles = MutableStateFlow(0)
    val publishedProfiles: StateFlow<Int> get() = _publishedProfiles.asStateFlow()
    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed
    val marketPrice: StateFlow<String> get() = marketPriceServiceFacade.selectedFormattedMarketPrice

    private val _numConnections = MutableStateFlow(0)
    val numConnections: StateFlow<Int> get() = _numConnections.asStateFlow()

    open val showNumConnections: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedNotifPermissionState: StateFlow<PermissionState?> =
        settingsRepository.data
            .mapLatest { it.notificationPermissionState }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                null,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedBatteryOptimizationState: StateFlow<BatteryOptimizationState?> =
        settingsRepository.data
            .mapLatest { it.batteryOptimizationState }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                null,
            )

    val isForeground get() = foregroundDetector.isForeground

    override fun onViewAttached() {
        super.onViewAttached()

        mainPresenter.setIsMainContentVisible(true)

        presenterScope.launch {
            mainPresenter.languageCode.collect {
                marketPriceServiceFacade.refreshSelectedFormattedMarketPrice()
            }
        }
        presenterScope.launch {
            offersServiceFacade.offerbookMarketItems.collect { items ->
                val totalOffers = items.sumOf { it.numOffers }
                _offersOnline.value = totalOffers
            }
        }
        presenterScope.launch {
            userProfileServiceFacade.numUserProfiles.collect {
                _publishedProfiles.value = it
            }
        }
        presenterScope.launch {
            networkServiceFacade.numConnections.collect {
                // numConnections in networkServiceFacade can be -1 (if no connections present at bootstrap),
                // but in UI we want to show always >= 0.
                _numConnections.value = it.coerceAtLeast(0)
            }
        }
    }

    fun onNavigateToMarkets() {
        disableInteractive()
        navigateToTradingTab()
        enableInteractive()
    }

    fun onOpenTradeGuide() {
        navigateTo(NavRoute.TradeGuideOverview)
    }

    private fun navigateToTradingTab() {
        navigateToTab(NavRoute.TabOfferbookMarket)
    }

    fun navigateLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }

    fun saveNotificationPermissionState(state: PermissionState) {
        presenterScope.launch { settingsRepository.setNotificationPermissionState(state) }
    }

    fun saveBatteryOptimizationState(state: BatteryOptimizationState) {
        presenterScope.launch { settingsRepository.setBatteryOptimizationPermissionState(state) }
    }

    suspend fun hasNotificationPermission(): Boolean = notificationController.hasPermission()
}
