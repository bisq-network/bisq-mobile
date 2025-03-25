package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter

class BuyerState4Presenter(
    mainPresenter: MainPresenter,
    tradesServiceFacade: TradesServiceFacade,
) : State4Presenter(mainPresenter, tradesServiceFacade) {

    override fun getMyDirectionString(): String {
        return "bisqEasy.tradeCompleted.header.myDirection.buyer".i18n() // I bought
    }

    override fun getMyOutcomeString(): String {
        return "bisqEasy.tradeCompleted.header.myOutcome.buyer".i18n() // I paid
    }
}