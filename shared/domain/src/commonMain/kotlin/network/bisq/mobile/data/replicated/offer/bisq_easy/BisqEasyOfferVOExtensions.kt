package network.bisq.mobile.data.replicated.offer.bisq_easy

import network.bisq.mobile.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.RangeAmountSpecVO

object BisqEasyOfferVOExtensions {
    fun BisqEasyOfferVO.getFixedOrMaxAmount() =
        when (val spec = amountSpec) {
            is FixedAmountSpecVO -> spec.amount
            is RangeAmountSpecVO -> spec.maxAmount
        }

    fun BisqEasyOfferVO.getFixedOrMinAmount() =
        when (val spec = amountSpec) {
            is FixedAmountSpecVO -> spec.amount
            is RangeAmountSpecVO -> spec.minAmount
        }
}
