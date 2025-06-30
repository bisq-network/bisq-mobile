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
    override val title: String = "mobile.nodeDashboard.title".i18n()
    override val bulletPoints: List<String> = listOf(
        "mobile.nodeDashboard.bulletPoint1".i18n(),
        "mobile.nodeDashboard.bulletPoint2".i18n(),
        "mobile.nodeDashboard.bulletPoint3".i18n(),
        "mobile.nodeDashboard.bulletPoint4".i18n(),
    )
}