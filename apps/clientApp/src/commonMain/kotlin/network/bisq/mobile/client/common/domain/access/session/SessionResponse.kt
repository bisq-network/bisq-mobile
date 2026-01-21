package network.bisq.mobile.client.common.domain.access.session

data class SessionResponse(
    val sessionId: String,
    val expiresAt: Long,
)
