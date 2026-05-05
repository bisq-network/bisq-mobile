package network.bisq.mobile.presentation.create_payment_account.select_payment_method.model

import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.model.account.getPaymentTypeVOFromCryptoCurrencyCode

data class CryptoPaymentMethodVO(
    override val paymentType: PaymentTypeVO,
    val code: String,
    override val name: String,
    val supportAutoConf: Boolean,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentMethodVO

fun CryptoPaymentMethod.toVO(): CryptoPaymentMethodVO? =
    getPaymentTypeVOFromCryptoCurrencyCode(code)?.let { paymentType ->
        CryptoPaymentMethodVO(
            paymentType = paymentType,
            code = code,
            name = name,
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = tradeLimitInfo,
            tradeDuration = tradeDuration,
        )
    }
