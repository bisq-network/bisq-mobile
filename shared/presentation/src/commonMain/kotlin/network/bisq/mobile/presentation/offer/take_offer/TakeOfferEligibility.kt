package network.bisq.mobile.presentation.offer.take_offer

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
