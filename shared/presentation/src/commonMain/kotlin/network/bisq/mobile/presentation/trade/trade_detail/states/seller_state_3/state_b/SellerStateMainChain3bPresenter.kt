package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b

import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.common.BaseTradeStateMainChain3bPresenter

class SellerStateMainChain3bPresenter(
    mainPresenter: MainPresenter,
    tradesServiceFacade: TradesServiceFacade,
    explorerServiceFacade: ExplorerServiceFacade
) : BaseTradeStateMainChain3bPresenter(
    mainPresenter,
    tradesServiceFacade,
    explorerServiceFacade
) {
    // This class is now a placeholder for any future Seller-specific logic.
}