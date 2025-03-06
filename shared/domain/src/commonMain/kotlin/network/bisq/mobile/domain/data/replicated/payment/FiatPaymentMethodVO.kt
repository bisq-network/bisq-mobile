package network.bisq.mobile.domain.data.replicated.payment


data class FiatPaymentMethodVO(
    override val name: String,
    override val paymentRail: FiatPaymentRailVO,
    override val displayString: String = name,
    override val shortDisplayString: String = name
) : PaymentMethodVO<FiatPaymentRailVO>(name, paymentRail, displayString, shortDisplayString) {

    companion object {
        fun fromPaymentRail(paymentRail: FiatPaymentRailVO): FiatPaymentMethodVO {
            return FiatPaymentMethodVO(paymentRail.name, paymentRail)
        }

        fun fromCustomName(customName: String): FiatPaymentMethodVO {
            return FiatPaymentMethodVO(customName, FiatPaymentRailVO.CUSTOM)
        }
    }
}