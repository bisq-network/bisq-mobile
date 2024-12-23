package network.bisq.mobile.domain.replicated.security.keys

import kotlinx.serialization.Serializable

@Serializable
data class KeyPairVO(val publicKey: PublicKeyVO, val privateKey: PrivateKeyVO)