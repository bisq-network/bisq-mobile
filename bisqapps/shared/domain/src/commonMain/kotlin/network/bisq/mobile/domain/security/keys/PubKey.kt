package network.bisq.mobile.domain.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class PubKey(
    val publicKey: String,
    val keyId: String
)