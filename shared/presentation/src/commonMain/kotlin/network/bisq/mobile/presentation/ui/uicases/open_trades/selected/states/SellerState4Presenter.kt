package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes


class SellerState4Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    fun onCloseTrade() {
        jobs.add(CoroutineScope(BackgroundDispatcher).launch {
            val result = tradesServiceFacade.closeTrade()
            when {
                // TODO review
                result.isFailure -> closeWorkflow()
                result.isSuccess -> closeWorkflow()
            }
        })
    }

    fun closeWorkflow() {
        // doing a shark navigateBack causes white broken UI screen
        navigateToTab(Routes.TabOpenTradeList)
    }

    fun onExportTradeDate() {
        jobs.add(CoroutineScope(BackgroundDispatcher).launch {
            tradesServiceFacade.exportTradeDate()
        })
    }
}