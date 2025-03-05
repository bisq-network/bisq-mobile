package network.bisq.mobile.domain.data.replicated.account

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.payment.PaymentMethodVO

@Serializable
abstract class AccountPayloadVO(
    open val id: String,
    open val paymentMethodName: String,
) {
    abstract fun verify()
}