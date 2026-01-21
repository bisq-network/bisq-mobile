package network.bisq.mobile.client.common.domain.access.session.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionResponseDto(
    val sessionId: String,
    val expiresAt: Long,
)
