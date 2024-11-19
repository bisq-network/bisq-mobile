package network.bisq.mobile.client.replicated_model.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class PubKey(
    val publicKey: String,
    val keyId: String
)