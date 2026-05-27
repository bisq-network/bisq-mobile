package network.bisq.mobile.client.common.presentation.model.account

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod

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
