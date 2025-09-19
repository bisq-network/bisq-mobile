package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.model.NotificationPermissionState
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.Routes

open class DashboardPresenter(
    private val mainPresenter: MainPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val notificationServiceController: NotificationServiceController,
) : BasePresenter(mainPresenter) {
    private val _offersOnline = MutableStateFlow(0)
    val offersOnline: StateFlow<Int> get() = _offersOnline.asStateFlow()

    private val _publishedProfiles = MutableStateFlow(0)
    val publishedProfiles: StateFlow<Int> get() = _publishedProfiles.asStateFlow()
    val numConnections: StateFlow<Int> get() = networkServiceFacade.numConnections
    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed
    val marketPrice: StateFlow<String> get() = marketPriceServiceFacade.selectedFormattedMarketPrice

    open val showNumConnections: Boolean = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedNotifPermissionState: StateFlow<NotificationPermissionState?> =
        settingsRepository.data.mapLatest { it.notificationPermissionState }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                null
            )

    override fun onViewAttached() {
        super.onViewAttached()

        collectUI(mainPresenter.languageCode) {
            marketPriceServiceFacade.refreshSelectedFormattedMarketPrice()
        }
        collectUI(offersServiceFacade.offerbookMarketItems) { items ->
            val totalOffers = items.sumOf { it.numOffers }
            _offersOnline.value = totalOffers
        }
        collectUI(userProfileServiceFacade.numUserProfiles) {
            _publishedProfiles.value = it
        }
    }

    fun onNavigateToMarkets() {
        disableInteractive()
        navigateToTradingTab()
        enableInteractive()
    }

    fun onOpenTradeGuide() {
        navigateTo(Routes.TradeGuideOverview)
    }

    private fun navigateToTradingTab() {
        navigateToTab(Routes.TabOfferbook)
    }

    fun navigateLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }

    fun saveNotificationPermissionState(state: NotificationPermissionState) {
        launchIO { settingsRepository.setNotificationPermissionState(state) }
    }

    fun doPlatformSpecificSetup() {
        notificationServiceController.doPlatformSpecificSetup()
    }

    suspend fun hasNotificationPermission(): Boolean = notificationServiceController.hasPermission()
}
