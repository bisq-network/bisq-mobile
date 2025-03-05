package network.bisq.mobile.domain.data.replicated.account

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.payment.PaymentMethodVO

@Serializable
open class AccountVO<P : AccountPayloadVO, M : PaymentMethodVO<*>>(
    open val accountName: String,
    open val paymentMethod: PaymentMethodVO<*>,
    open val accountPayload: AccountPayloadVO
)