package network.bisq.mobile.domain.data.replicated.offer.bisq_easy

import network.bisq.mobile.domain.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO

object BisqEasyOfferVOExtensions {
    fun BisqEasyOfferVO.getFixedOrMaxAmount() = if (amountSpec is FixedAmountSpecVO) {
        amountSpec.amount
    } else {
        (amountSpec as? RangeAmountSpecVO)?.maxAmount
            ?: throw IllegalArgumentException("Unexpected amountSpec type: ${amountSpec::class.simpleName}")
    }

    fun BisqEasyOfferVO.getFixedOrMinAmount() = if (amountSpec is FixedAmountSpecVO) {
        amountSpec.amount
    } else {
        (amountSpec as? RangeAmountSpecVO)?.minAmount
            ?: throw IllegalArgumentException("Unexpected amountSpec type: ${amountSpec::class.simpleName}")
    }
}