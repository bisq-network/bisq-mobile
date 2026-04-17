package network.bisq.mobile.presentation.trade.trade_detail

sealed interface TradeDetailsHeaderUiAction {
    data object ToggleHeader : TradeDetailsHeaderUiAction

    data object OpenInterruptionConfirmationDialog : TradeDetailsHeaderUiAction

    data object OpenMediationConfirmationDialog : TradeDetailsHeaderUiAction
}
