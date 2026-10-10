package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

sealed interface BuyerState2aUiAction {
    data object OnConfirmFiatSent : BuyerState2aUiAction

    data object OnAcknowledgeBannedWarning : BuyerState2aUiAction

    data object OnRetryBannedCancel : BuyerState2aUiAction
}
