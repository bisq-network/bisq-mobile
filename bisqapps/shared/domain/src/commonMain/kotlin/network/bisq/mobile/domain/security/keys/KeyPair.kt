package network.bisq.mobile.domain.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class KeyPair(
    val privateKey: String,
    val publicKey: String
)