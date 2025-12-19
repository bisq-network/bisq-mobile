package network.bisq.mobile.domain.data.replicated.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class I2pKeyPairVO(
    // Base64 encoded byte[]
    val identityBytes: String,
    // Base64 encoded byte[]
    val destinationBytes: String,
)
