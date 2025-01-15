package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class SellerStateLightning3bPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    private var job: Job? = null
    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
        job?.cancel()
        job = null
    }

    fun onCompleteTrade() {
        job = CoroutineScope(BackgroundDispatcher).launch {
            tradesServiceFacade.btcConfirmed()
        }
    }
}