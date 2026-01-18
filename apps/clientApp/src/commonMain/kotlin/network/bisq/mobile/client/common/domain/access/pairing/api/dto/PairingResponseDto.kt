package network.bisq.mobile.client.common.domain.access.pairing.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingResponseDto(
    val sessionId: String,
    val expiresAt: Long
)

