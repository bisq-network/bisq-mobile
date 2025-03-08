package network.bisq.mobile.domain.data.replicated.account.payment_method

// Not used now
//data class FiatPaymentMethodVO(
//    override val name: String,
//    override val paymentRail: FiatPaymentRailEnum,
//    override val displayString: String = name,
//    override val shortDisplayString: String = name
//) : PaymentMethodVO<FiatPaymentRailEnum>(name, paymentRail, displayString, shortDisplayString) {
//
//    companion object {
//        fun fromPaymentRail(paymentRail: FiatPaymentRailEnum): FiatPaymentMethodVO {
//            return FiatPaymentMethodVO(paymentRail.name, paymentRail)
//        }
//
//        fun fromCustomName(customName: String): FiatPaymentMethodVO {
//            return FiatPaymentMethodVO(customName, FiatPaymentRailEnum.CUSTOM)
//        }
//    }
//}