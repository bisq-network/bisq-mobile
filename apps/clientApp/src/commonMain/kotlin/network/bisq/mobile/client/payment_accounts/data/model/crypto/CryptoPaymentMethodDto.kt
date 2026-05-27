package network.bisq.mobile.client.payment_accounts.data.model.crypto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoPaymentMethodDto(
    val code: String,
    val name: String,
    val supportAutoConf: Boolean,
    val tradeLimitInfo: String,
    val tradeDuration: String,
)
