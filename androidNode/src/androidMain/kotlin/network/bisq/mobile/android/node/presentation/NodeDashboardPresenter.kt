package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter

class NodeDashboardPresenter(
    mainPresenter: MainPresenter,
    profileStatsServiceFacade: ProfileStatsServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    offersServiceFacade: OffersServiceFacade
) : DashboardPresenter(mainPresenter, profileStatsServiceFacade, marketPriceServiceFacade, offersServiceFacade) {
    override val titleKey: String = "mobile.nodeDashboard.title"
    override val bulletPointsKey: List<String> = listOf(
        "mobile.nodeDashboard.bulletPoint1",
        "mobile.nodeDashboard.bulletPoint2",
        "mobile.nodeDashboard.bulletPoint3",
        "mobile.nodeDashboard.bulletPoint4",
    )
}