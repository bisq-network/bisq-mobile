package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class BuyerState3aPresenter(
    mainPresenter: MainPresenter,
    tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade
}