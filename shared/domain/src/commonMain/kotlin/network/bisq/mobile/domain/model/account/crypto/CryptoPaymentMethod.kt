package network.bisq.mobile.domain.model.account.crypto

import network.bisq.mobile.domain.model.account.PaymentMethod

data class CryptoPaymentMethod(
    val code: String,
    override val name: String,
    val supportAutoConf: Boolean,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentMethod
