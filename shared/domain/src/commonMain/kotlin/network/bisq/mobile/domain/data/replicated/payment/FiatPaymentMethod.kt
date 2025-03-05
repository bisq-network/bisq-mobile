package network.bisq.mobile.domain.data.replicated.payment


data class FiatPaymentMethod(
    override val name: String,
    override val paymentRail: FiatPaymentRail,
    override val displayString: String = name,
    override val shortDisplayString: String = name
) : PaymentMethodVO<FiatPaymentRail>(name, paymentRail, displayString, shortDisplayString) {

    companion object {
        fun fromPaymentRail(paymentRail: FiatPaymentRail): FiatPaymentMethod {
            return FiatPaymentMethod(paymentRail.name, paymentRail)
        }

        fun fromCustomName(customName: String): FiatPaymentMethod {
            return FiatPaymentMethod(customName, FiatPaymentRail.CUSTOM)
        }
    }
}