package network.bisq.mobile.presentation.offer.take_offer.btc_address

data class TakeOfferBtcAddressUiState(
    val address: String = "",
    val isValid: Boolean = false,
    val showBarcodeView: Boolean = false,
    val showBarcodeError: Boolean = false,
    /** Bumped to make the address field re-run its visual validation after a scan seeds the value. */
    val validationTrigger: Int = 0,
)

sealed interface TakeOfferBtcAddressUiAction {
    data class OnAddressInput(
        val value: String,
        val isValid: Boolean,
    ) : TakeOfferBtcAddressUiAction

    data object OnNext : TakeOfferBtcAddressUiAction

    data object OnBack : TakeOfferBtcAddressUiAction

    data object OnClose : TakeOfferBtcAddressUiAction

    data object OnOpenWalletGuide : TakeOfferBtcAddressUiAction

    data object OnBarcodeClick : TakeOfferBtcAddressUiAction

    data object OnBarcodeDismiss : TakeOfferBtcAddressUiAction

    data object OnBarcodeFail : TakeOfferBtcAddressUiAction

    data object OnBarcodeErrorClose : TakeOfferBtcAddressUiAction

    data class OnBarcodeResult(
        val value: String,
    ) : TakeOfferBtcAddressUiAction
}
