package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

sealed interface BuyerState1aUiAction {
    data class OnPaymentDataInput(
        val value: String,
        val isValid: Boolean,
    ) : BuyerState1aUiAction

    data class OnConfirmPrefillChange(
        val checked: Boolean,
    ) : BuyerState1aUiAction

    data object OnSend : BuyerState1aUiAction

    /** "Proceed anyway" in the invalid address dialog. */
    data object OnSendAnyway : BuyerState1aUiAction

    data object OnDismissInvalidAddressDialog : BuyerState1aUiAction

    data object OnOpenWalletGuide : BuyerState1aUiAction

    data object OnBarcodeClick : BuyerState1aUiAction

    data object OnBarcodeDismiss : BuyerState1aUiAction

    data object OnBarcodeFail : BuyerState1aUiAction

    data object OnBarcodeErrorClose : BuyerState1aUiAction

    data class OnBarcodeResult(
        val value: String,
    ) : BuyerState1aUiAction
}
