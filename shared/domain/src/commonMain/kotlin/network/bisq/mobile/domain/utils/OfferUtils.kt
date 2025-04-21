package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO

object OfferUtils {
    fun getFixedOrMaxAmount(offer: BisqEasyOfferVO) = if (offer.amountSpec is FixedAmountSpecVO) {
        offer.amountSpec.amount
    } else {
        (offer.amountSpec as? RangeAmountSpecVO)?.maxAmount
            ?: throw IllegalArgumentException("Unexpected amountSpec type: ${offer.amountSpec::class.simpleName}")
    }

    fun getFixedOrMinAmount(offer: BisqEasyOfferVO) = if (offer.amountSpec is FixedAmountSpecVO) {
        offer.amountSpec.amount
    } else {
        (offer.amountSpec as? RangeAmountSpecVO)?.minAmount
            ?: throw IllegalArgumentException("Unexpected amountSpec type: ${offer.amountSpec::class.simpleName}")
    }
}