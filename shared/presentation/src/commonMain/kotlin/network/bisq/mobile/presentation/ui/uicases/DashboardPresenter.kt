package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    override val offersOnline: StateFlow<Int> = _offersOnline

    private val _publishedProfiles = MutableStateFlow(0)
    override val publishedProfiles: StateFlow<Int> = _publishedProfiles

    val formattedMarketPrice: StateFlow<String> = marketPriceServiceFacade.selectedFormattedMarketPrice

    override fun onViewAttached() {
        super.onViewAttached()
        collectUI(offersServiceFacade.offerbookMarketItems) { items ->
            _offersOnline.value = items.sumOf { it.numOffers.value }
        }
        collectUI(profileStatsServiceFacade.publishedProfilesCount) { count ->
            _publishedProfiles.value = count
            log.d { "DashboardPresenter: NetworkStats publishedProfilesCount received: $count" }
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