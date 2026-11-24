package network.bisq.mobile.presentation.offer.take_offer

import network.bisq.mobile.domain.service.trades.TakeOfferRefusal
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.offer.MissingPriceQuoteException

/**
 * Result of the reputation-based take-offer eligibility check
 * ([TakeOfferCoordinator.checkTakeOfferEligibility]).
 *
 * [NotEnoughReputation] carries the ready-to-render dialog copy because the wording depends on
 * which side is the seller (see the check itself) — computing it where the scores are at hand
 * keeps every entry point (offerbook, peer profile) showing identical explanations.
 */
sealed class TakeOfferEligibility {
    data object Eligible : TakeOfferEligibility()

    data class NotEnoughReputation(
        val headline: String,
        val message: String,
        /** True when the taker would be the seller (BUY offer) — their own score is the one lacking. */
        val isSellerAsTakerWarning: Boolean,
    ) : TakeOfferEligibility()
}

/**
 * The snackbar text when taking [offerId] cannot start: missing market prices get their own
 * message, which tells the user to retry later; anything else the generic one.
 */
fun takeOfferStartFailureMessage(
    error: Throwable,
    offerId: String,
): String =
    if (error is MissingPriceQuoteException) {
        TakeOfferRefusal.MARKET_PRICE_NOT_CURRENT.message()
    } else {
        "mobile.bisqEasy.offerbook.unableToTakeOffer".i18n(offerId)
    }
