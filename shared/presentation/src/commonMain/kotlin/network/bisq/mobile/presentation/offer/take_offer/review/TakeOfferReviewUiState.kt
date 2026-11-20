package network.bisq.mobile.presentation.offer.take_offer.review

import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsUiState

data class TakeOfferReviewUiState(
    val headLine: String,
    val takersDirection: DirectionEnum,
    val amountToPay: String,
    val amountToReceive: String,
    val price: String,
    val marketCodes: String,
    /** The offer card's wording for the price type: "Fixed", "+2.00 % vs market" or "Market price". */
    val priceTypeLabel: String,
    val priceDetails: String,
    val quoteSidePaymentMethodDisplayString: String,
    val baseSidePaymentMethodDisplayString: String,
    val fee: String,
    val feeDetails: String,
    val addressNotice: AddressNotice? = null,
    /** The terms attached to the offer, which the taker accepts by taking it. Null when there are none. */
    val tradeTerms: TradeTermsUiState? = null,
    val showTakeOfferProgressDialog: Boolean = false,
    val showTakeOfferSuccessDialog: Boolean = false,
    val takeOfferErrorDialog: TakeOfferErrorDialog? = null,
    val isSupportChannelAvailable: Boolean = false,
) {
    /**
     * Which payout-address notice the review shows to a buyer, or null for none.
     * Selection happens once when the presenter is created — the wizard model cannot change
     * while review is on screen.
     */
    sealed interface AddressNotice {
        /** Mainchain, no address collected, first-time trader: announce the upcoming ask. */
        data object MainchainAnnounce : AddressNotice

        /** Mainchain, address collected in the wizard step: confirm rather than warn. */
        data class MainchainConfirmed(
            val truncatedAddress: String,
        ) : AddressNotice

        /**
         * Lightning trades announce for everyone, veterans included: invoice expiry timing is
         * operationally relevant on every trade, not a beginner explainer.
         */
        data object Lightning : AddressNotice
    }
}

sealed interface TakeOfferReviewUiAction {
    data object OnBack : TakeOfferReviewUiAction

    data object OnClose : TakeOfferReviewUiAction

    data object OnTakeOffer : TakeOfferReviewUiAction

    data object OnGoToOpenTrades : TakeOfferReviewUiAction

    data object OnDismissTakeOfferError : TakeOfferReviewUiAction

    data object OnOpenSupportChannel : TakeOfferReviewUiAction

    data object OnEditAddress : TakeOfferReviewUiAction

    data object OnOpenWalletGuide : TakeOfferReviewUiAction

    data object OnToggleTradeTerms : TakeOfferReviewUiAction
}
