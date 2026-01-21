package network.bisq.mobile.client.common.domain.access.pairing.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingResponseDto(
    val version: Byte,
    val clientId: String,
    val clientSecret: String,
    val sessionId: String,
    val sessionExpiryDate: Long,
)
