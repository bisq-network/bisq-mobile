package network.bisq.mobile.domain.data.replicated.account

import network.bisq.mobile.domain.data.replicated.payment.FiatPaymentMethodVO


data class StrikeAccountVO(
    override val accountName: String,
    override val accountPayload: StrikeAccountPayloadVO,
    val country: String
) : AccountVO<StrikeAccountPayloadVO, FiatPaymentMethodVO>(accountName, FiatPaymentMethodVO.fromCustomName("CUSTOM"), accountPayload)
