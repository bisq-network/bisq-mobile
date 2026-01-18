package network.bisq.mobile.client.common.domain.access.pairing

data class PairingResponse(
    val sessionId: String,
    val expiresAt: Long
)

