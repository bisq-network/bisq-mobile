package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoPaymentMethodItemDto(
    val code: String,
    val name: String,
    val category: String,
)
