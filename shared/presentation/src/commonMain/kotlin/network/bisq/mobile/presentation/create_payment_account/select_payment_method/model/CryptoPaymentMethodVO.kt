package network.bisq.mobile.presentation.create_payment_account.select_payment_method.model

import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.model.account.getPaymentMethodVOFromCryptoCurrencyCode

data class CryptoPaymentMethodVO(
    val paymentMethod: PaymentMethodVO,
    val code: String,
    val name: String,
)

fun CryptoPaymentMethod.toVO(): CryptoPaymentMethodVO? =
    getPaymentMethodVOFromCryptoCurrencyCode(code)?.let { paymentMethod ->
        CryptoPaymentMethodVO(
            paymentMethod = paymentMethod,
            code = code,
            name = name,
        )
    }
