package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network_stats.NetworkStatsServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter

class NodeDashboardPresenter(
    mainPresenter: MainPresenter,
    networkStatsServiceFacade: NetworkStatsServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    offersServiceFacade: OffersServiceFacade
) : DashboardPresenter(mainPresenter, networkStatsServiceFacade, marketPriceServiceFacade, offersServiceFacade) {
}