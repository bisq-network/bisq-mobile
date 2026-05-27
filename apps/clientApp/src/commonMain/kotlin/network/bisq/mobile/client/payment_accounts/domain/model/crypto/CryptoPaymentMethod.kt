package network.bisq.mobile.client.payment_accounts.domain.model.crypto

import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod

data class CryptoPaymentMethod(
    val code: String,
    override val name: String,
    val supportAutoConf: Boolean,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentMethod
