package network.bisq.mobile.domain.data.replicated.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class TorKeyPairVO(val privateKeyEncoded: String, val publicKeyEncoded: String, val onionAddress: String)