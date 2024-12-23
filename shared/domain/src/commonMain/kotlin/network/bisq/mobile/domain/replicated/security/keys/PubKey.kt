package network.bisq.mobile.domain.replicated.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class PubKey(
    val publicKey: String,
    val keyId: String
)