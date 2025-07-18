package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter

class NodeDashboardPresenter(
    mainPresenter: MainPresenter,
    bisqStatsRepository: BisqStatsRepository,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    offersServiceFacade: OffersServiceFacade
) : DashboardPresenter(mainPresenter, bisqStatsRepository, marketPriceServiceFacade, offersServiceFacade) {
    override val titleKey: String = "mobile.nodeDashboard.title"
    override val bulletPointsKey: List<String> = listOf(
        "mobile.nodeDashboard.bulletPoint1",
        "mobile.nodeDashboard.bulletPoint2",
        "mobile.nodeDashboard.bulletPoint3",
        "mobile.nodeDashboard.bulletPoint4",
    )
}