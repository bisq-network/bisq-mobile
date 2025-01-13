package network.bisq.mobile.domain.data.replicated.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class KeyBundleVO(
    val keyId: String,
    val keyPair: KeyPairVO,
    val torKeyPair: TorKeyPairVO
)