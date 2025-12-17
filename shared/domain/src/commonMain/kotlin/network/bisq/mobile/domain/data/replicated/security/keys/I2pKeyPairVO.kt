package network.bisq.mobile.domain.data.replicated.security.keys

import kotlinx.serialization.Serializable

// TODO implement proper properties
@Serializable
data class I2pKeyPairVO(val privateKeyEncoded: String, val publicKeyEncoded: String, val onionAddress: String)