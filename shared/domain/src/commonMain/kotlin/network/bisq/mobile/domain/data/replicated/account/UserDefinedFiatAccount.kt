package network.bisq.mobile.domain.data.replicated.account

import network.bisq.mobile.domain.data.replicated.payment.FiatPaymentMethod

data class UserDefinedFiatAccount(
    override val accountName: String,
    override val paymentMethod: FiatPaymentMethod,
    override val accountPayload: UserDefinedFiatAccountPayload
) : AccountVO<UserDefinedFiatAccountPayload, FiatPaymentMethod>(accountName, paymentMethod, accountPayload)
