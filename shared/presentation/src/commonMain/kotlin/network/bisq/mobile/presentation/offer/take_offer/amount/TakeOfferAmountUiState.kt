package network.bisq.mobile.presentation.offer.take_offer.amount

data class TakeOfferAmountUiState(
    val quoteCurrencyCode: String = "",
    /** Lower bound without currency code, for the description text. */
    val formattedMinAmount: String = "",
    val formattedMinAmountWithCode: String = "",
    val formattedMaxAmountWithCode: String = "",
    val formattedQuoteAmount: String = "",
    val formattedBaseAmount: String = "",
    val sliderPosition: Float = 0.5f,
    val amountValid: Boolean = false,
)

sealed interface TakeOfferAmountUiAction {
    data class OnSliderValueChange(
        val position: Float,
    ) : TakeOfferAmountUiAction

    data object OnSliderDragFinished : TakeOfferAmountUiAction

    data class OnTextValueChange(
        val text: String,
    ) : TakeOfferAmountUiAction

    data object OnNext : TakeOfferAmountUiAction

    data object OnBack : TakeOfferAmountUiAction

    data object OnClose : TakeOfferAmountUiAction
}
