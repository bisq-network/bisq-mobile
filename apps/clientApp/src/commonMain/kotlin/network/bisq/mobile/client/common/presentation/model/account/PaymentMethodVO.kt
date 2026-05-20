package network.bisq.mobile.client.common.presentation.model.account

import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod

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
