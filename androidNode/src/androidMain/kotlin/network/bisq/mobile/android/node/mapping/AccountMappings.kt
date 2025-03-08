package network.bisq.mobile.android.node.mapping

import bisq.account.accounts.*
import bisq.account.payment_method.FiatPaymentMethod
import bisq.account.payment_method.FiatPaymentRail
import bisq.account.payment_method.PaymentMethod
import bisq.common.locale.Country
import bisq.common.locale.Region
//import network.bisq.mobile.domain.data.replicated.account.*
//import network.bisq.mobile.domain.data.replicated.account.StrikeAccountVO
//import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
//import network.bisq.mobile.domain.data.replicated.account.ZelleAccountVO
//import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentMethodVO
//import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailVO
//import network.bisq.mobile.domain.data.replicated.account.payment_method.PaymentMethodVO

// Not used now
//object AccountMapping {
//    fun <P : AccountPayloadVO, M : PaymentMethodVO<*>> toBisq2Model(value: AccountVO<P, M>): Account<*, *> {
//        return when (value) {
//            is UserDefinedFiatAccountVO -> UserDefinedFiatAccountMapping.toBisq2Model(value)
//            is StrikeAccountVO -> StrikeAccountMapping.toBisq2Model(value)
//            is ZelleAccountVO -> ZelleAccountMapping.toBisq2Model(value)
//            else -> throw IllegalArgumentException("Unsupported AccountVO type: ${value::class}")
//        }
//    }
//
//    fun <P : AccountPayload, M : PaymentMethod<*>> fromBisq2Model(value: Account<P, M>): AccountVO<*, *> {
//        return when (value) {
//            is UserDefinedFiatAccount -> UserDefinedFiatAccountMapping.fromBisq2Model(value)
//            is StrikeAccount -> StrikeAccountMapping.fromBisq2Model(value)
//            is ZelleAccount -> ZelleAccountMapping.fromBisq2Model(value)
//            else -> throw IllegalArgumentException("Unsupported Account type: ${value::class}")
//        }
//    }
//}
//
//object UserDefinedFiatAccountMapping {
//    fun toBisq2Model(value: UserDefinedFiatAccountVO): UserDefinedFiatAccount {
//        return UserDefinedFiatAccount(
//            value.accountName,
//            value.accountPayload.accountData
//        )
//    }
//
//    fun fromBisq2Model(value: UserDefinedFiatAccount): UserDefinedFiatAccountVO {
//        return UserDefinedFiatAccountVO(
//            value.accountName,
//            FiatPaymentMethodMapping.fromBisq2Model(value.paymentMethod),
//            UserDefinedFiatAccountPayloadMapping.fromBisq2Model(value.accountPayload)
//        )
//    }
//}
//
//object StrikeAccountMapping {
//    fun toBisq2Model(value: StrikeAccountVO): StrikeAccount {
//        return StrikeAccount(
//            value.accountName,
//            StrikeAccountPayloadMapping.toBisq2Model(value.accountPayload),
//            Country(value.country, value.country, Region("", ""))
//        )
//    }
//
//    fun fromBisq2Model(value: StrikeAccount): StrikeAccountVO {
//        return StrikeAccountVO(
//            value.accountName,
//            StrikeAccountPayloadMapping.fromBisq2Model(value.accountPayload),
//            value.country.code
//        )
//    }
//}
//
//object ZelleAccountMapping {
//    fun toBisq2Model(value: ZelleAccountVO): ZelleAccount {
//        return ZelleAccount(
//            0,
//            value.accountName,
//            ZelleAccountPayloadMapping.toBisq2Model(value.accountPayload)
//        )
//    }
//
//    fun fromBisq2Model(value: ZelleAccount): ZelleAccountVO {
//        return ZelleAccountVO(
//            value.accountName,
//            FiatPaymentMethodMapping.fromBisq2Model(value.paymentMethod),
//            ZelleAccountPayloadMapping.fromBisq2Model(value.accountPayload)
//        )
//    }
//}
//
//object AccountPayloadMapping {
//    fun toBisq2Model(value: AccountPayloadVO): AccountPayload {
//        return when (value) {
//            is UserDefinedFiatAccountPayloadVO -> UserDefinedFiatAccountPayloadMapping.toBisq2Model(value)
//            is StrikeAccountPayloadVO -> StrikeAccountPayloadMapping.toBisq2Model(value)
//            is ZelleAccountPayloadVO -> ZelleAccountPayloadMapping.toBisq2Model(value)
//            else -> throw IllegalArgumentException("Unsupported AccountPayloadVO type: ${value::class}")
//        }
//    }
//
//    fun fromBisq2Model(value: AccountPayload): AccountPayloadVO {
//        return when (value) {
//            is UserDefinedFiatAccountPayload -> UserDefinedFiatAccountPayloadMapping.fromBisq2Model(value)
//            is StrikeAccountPayload -> StrikeAccountPayloadMapping.fromBisq2Model(value)
//            is ZelleAccountPayload -> ZelleAccountPayloadMapping.fromBisq2Model(value)
//            else -> throw IllegalArgumentException("Unsupported AccountPayload type: ${value::class}")
//        }
//    }
//}
//
//object UserDefinedFiatAccountPayloadMapping {
//    fun toBisq2Model(value: UserDefinedFiatAccountPayloadVO): UserDefinedFiatAccountPayload {
//        return UserDefinedFiatAccountPayload(value.id, value.paymentMethodName, value.accountData)
//    }
//
//    fun fromBisq2Model(value: UserDefinedFiatAccountPayload): UserDefinedFiatAccountPayloadVO {
//        return UserDefinedFiatAccountPayloadVO(value.id, value.paymentMethodName, value.accountData)
//    }
//}
//
//object StrikeAccountPayloadMapping {
//    fun toBisq2Model(value: StrikeAccountPayloadVO): StrikeAccountPayload {
//        return StrikeAccountPayload(value.id, value.paymentMethodName, value.countryCode, value.holderName)
//    }
//
//    fun fromBisq2Model(value: StrikeAccountPayload): StrikeAccountPayloadVO {
//        return StrikeAccountPayloadVO(value.id, value.paymentMethodName, value.countryCode, value.holderName)
//    }
//}
//
//object ZelleAccountPayloadMapping {
//    fun toBisq2Model(value: ZelleAccountPayloadVO): ZelleAccountPayload {
//        return ZelleAccountPayload(value.id, value.paymentMethodName, value.emailOrMobileNr, value.holderName)
//    }
//
//    fun fromBisq2Model(value: ZelleAccountPayload): ZelleAccountPayloadVO {
//        return ZelleAccountPayloadVO(value.id, value.paymentMethodName, value.emailOrMobileNr, value.holderName)
//    }
//}
//
//
//object PaymentMethodMapping {
//    fun toBisq2Model(value: PaymentMethodVO<*>): PaymentMethod<*> {
//        return when (value) {
//            is FiatPaymentMethodVO -> FiatPaymentMethodMapping.toBisq2Model(value)
//            else -> throw IllegalArgumentException("Unsupported PaymentMethodVO type: ${value::class}")
//        }
//    }
//
//    fun fromBisq2Model(value: PaymentMethod<*>): PaymentMethodVO<*> {
//        return when (value) {
//            is FiatPaymentMethod -> FiatPaymentMethodMapping.fromBisq2Model(value)
//            else -> throw IllegalArgumentException("Unsupported PaymentMethod type: ${value::class}")
//        }
//    }
//}
//
//object FiatPaymentMethodMapping {
//    fun toBisq2Model(value: FiatPaymentMethodVO): FiatPaymentMethod {
//        return FiatPaymentMethod.fromCustomName(value.name)
//    }
//
//    fun fromBisq2Model(value: FiatPaymentMethod): FiatPaymentMethodVO {
//        return FiatPaymentMethodVO(value.name, FiatPaymentRailMapping.fromBisq2Model(value.paymentRail))
//    }
//}
//
//object FiatPaymentRailMapping {
//    fun toBisq2Model(value: FiatPaymentRailVO): FiatPaymentRail {
//        return when (value) {
//            FiatPaymentRailVO.CUSTOM -> FiatPaymentRail.CUSTOM
//        }
//    }
//
//    fun fromBisq2Model(value: FiatPaymentRail): FiatPaymentRailVO {
//        return when (value) {
//            FiatPaymentRail.CUSTOM -> FiatPaymentRailVO.CUSTOM
//            else -> FiatPaymentRailVO.CUSTOM // TODO
//        }
//    }
//}
