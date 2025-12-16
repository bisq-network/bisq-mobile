package network.bisq.mobile.presentation.offerbook

import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel

/**
 * These are pure helpers so they are easy to unit test.
 */
object OfferbookFilterDerivations {
    fun paymentMethodIds(offers: List<OfferItemPresentationModel>): Set<String> =
        offers.asSequence().flatMap { it.quoteSidePaymentMethods.asSequence() }.toSet()

    fun settlementMethodIds(offers: List<OfferItemPresentationModel>): Set<String> =
        offers.asSequence().flatMap { it.baseSidePaymentMethods.asSequence() }.toSet()
}

