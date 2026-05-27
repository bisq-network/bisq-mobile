package network.bisq.mobile.client.common.presentation.model.account

import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod

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
