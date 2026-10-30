package network.bisq.mobile.presentation.trade.trade_detail

sealed interface InterruptedTradeUiAction {
    data object OnCloseTrade : InterruptedTradeUiAction

    data object OnReportToMediator : InterruptedTradeUiAction

    data object OnDismissNoMediatorAvailableWarning : InterruptedTradeUiAction

    data object OnDismissMediationRequestedDialog : InterruptedTradeUiAction
}
