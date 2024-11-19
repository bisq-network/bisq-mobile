package network.bisq.mobile.client.replicated_model.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class KeyPair(
    val privateKey: String,
    val publicKey: String
)