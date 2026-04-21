package network.bisq.mobile.presentation.trade.trade_detail.states.common

sealed interface State4UiAction {
    data object OnExportTradeClick : State4UiAction

    data object OnCloseTradeClick : State4UiAction

    data object OnDismissCloseTrade : State4UiAction

    data object OnConfirmCloseTrade : State4UiAction
}
