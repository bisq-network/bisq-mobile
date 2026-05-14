package network.bisq.mobile.presentation.create_payment_account.select_payment_method.model

import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO

interface PaymentMethodVO {
    val paymentType: PaymentTypeVO
    val name: String
    val tradeLimitInfo: String
    val tradeDuration: String
}

fun PaymentMethod.toVO(): PaymentMethodVO? =
    when (this) {
        is FiatPaymentMethod -> toVO()
        is CryptoPaymentMethod -> toVO()
        else -> null
    }
