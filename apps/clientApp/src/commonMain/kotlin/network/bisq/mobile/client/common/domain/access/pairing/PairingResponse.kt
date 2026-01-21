package network.bisq.mobile.client.common.domain.access.pairing

data class PairingResponse(
    val version: Byte,
    val clientId: String,
    val clientSecret: String,
    val sessionId: String,
    val sessionExpiryDate: Long,
)
