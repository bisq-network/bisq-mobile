package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.Routes

open class DashboardPresenter(
    private val mainPresenter: MainPresenter,
    private val profileStatsServiceFacade: ProfileStatsServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade
) : BasePresenter(mainPresenter), IGettingStarted {
    override val titleKey: String = "mobile.dashboard.title"

    override val bulletPointsKey: List<String> = listOf(
        "mobile.dashboard.bulletPoint1",
        "mobile.dashboard.bulletPoint2",
        "mobile.dashboard.bulletPoint3",
    )

    private val _offersOnline = MutableStateFlow(0)
    override val offersOnline: StateFlow<Int> get() = _offersOnline.asStateFlow()

    private val _publishedProfiles = MutableStateFlow(0)
    override val publishedProfiles: StateFlow<Int> get() = _publishedProfiles.asStateFlow()

    val formattedMarketPrice: StateFlow<String> get() = marketPriceServiceFacade.selectedFormattedMarketPrice

    override fun onViewAttached() {
        super.onViewAttached()
        collectUI(offersServiceFacade.offerbookMarketItems) { items ->
            val totalOffers = items?.sumOf { it.numOffers } ?: 0
            _offersOnline.value = totalOffers
            log.d { "DashboardPresenter: Updated offers online count: $totalOffers (items: ${items?.size ?: 0})" }
        }
        collectUI(profileStatsServiceFacade.publishedProfilesCount) { count ->
            val safeCount = count ?: 0
            _publishedProfiles.value = safeCount
            log.d { "DashboardPresenter: NetworkStats publishedProfilesCount received: $safeCount" }
        }
        collectUI(mainPresenter.languageCode) {
            marketPriceServiceFacade.refreshSelectedFormattedMarketPrice()
        }
    }

    override fun onStartTrading() {
        disableInteractive()
        navigateToTradingTab()
        enableInteractive()
    }

    private fun navigateToTradingTab() {
        navigateToTab(Routes.TabOfferbook)
    }

    override fun navigateLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }
}