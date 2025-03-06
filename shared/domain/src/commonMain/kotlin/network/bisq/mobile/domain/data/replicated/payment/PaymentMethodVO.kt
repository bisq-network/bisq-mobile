package network.bisq.mobile.domain.data.replicated.payment

import kotlinx.serialization.Serializable

@Serializable
abstract class PaymentMethodVO<R : PaymentRailVO>(
    open val name: String,
    open val paymentRail: R,
    open val displayString: String,
    open val shortDisplayString: String
)