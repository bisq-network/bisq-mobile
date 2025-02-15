package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class SellerState4Presenter(
    mainPresenter: MainPresenter,
    tradesServiceFacade: TradesServiceFacade,
) : State4Presenter(mainPresenter, tradesServiceFacade)