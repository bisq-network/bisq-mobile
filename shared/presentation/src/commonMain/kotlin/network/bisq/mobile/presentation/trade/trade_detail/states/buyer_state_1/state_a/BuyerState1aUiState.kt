package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType

data class BuyerState1aUiState(
    val headline: String = "",
    val description: String = "",
    val paymentData: String = "",
    val isPaymentDataValid: Boolean = false,
    val addressFieldType: BitcoinLnAddressFieldType = BitcoinLnAddressFieldType.Bitcoin,
    /** Bumped to make the address field re-run its visual validation after the presenter sets the value. */
    val validationTrigger: Int = 0,
    /** The field holds the address collected at take-offer time, unedited. */
    val wasPrefilled: Boolean = false,
    val hasConfirmedPrefill: Boolean = false,
    val showInvalidAddressDialog: Boolean = false,
    /** The data requests another amount than the trade amount, which the seller's node rejects. */
    val showOtherAmountWarning: Boolean = false,
    /** The trade amount for the other amount warning, e.g. "0.0025 BTC". */
    val tradeAmountWithCode: String = "",
    val showBarcodeView: Boolean = false,
    val showBarcodeError: Boolean = false,
)
