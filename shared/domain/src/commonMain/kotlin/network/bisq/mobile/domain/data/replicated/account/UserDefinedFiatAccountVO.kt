package network.bisq.mobile.domain.data.replicated.account

import network.bisq.mobile.domain.data.replicated.payment.FiatPaymentMethodVO

data class UserDefinedFiatAccountVO(
    override val accountName: String,
    override val paymentMethod: FiatPaymentMethodVO,
    override val accountPayload: UserDefinedFiatAccountPayloadVO
) : AccountVO<UserDefinedFiatAccountPayloadVO, FiatPaymentMethodVO>(accountName, paymentMethod, accountPayload)
