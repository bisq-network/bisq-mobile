package network.bisq.mobile.domain.data.replicated.account

import network.bisq.mobile.domain.data.replicated.payment.FiatPaymentMethodVO

data class ZelleAccountVO(
    override val accountName: String,
    override val paymentMethod: FiatPaymentMethodVO,
    override val accountPayload: ZelleAccountPayloadVO
) : AccountVO<ZelleAccountPayloadVO, FiatPaymentMethodVO>(accountName, paymentMethod, accountPayload)
