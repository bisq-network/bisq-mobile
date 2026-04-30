package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoPaymentMethodDto(
    val code: String,
    val name: String,
    val category: String,
    val supportAutoConf: Boolean,
    val restrictions: String,
)
