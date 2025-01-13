package network.bisq.mobile.client.service.user_profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.security.keys.KeyPairVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO

@Serializable
data class KeyMaterialResponse(
    val keyPair: KeyPairVO,
    val id: String,
    val nym: String,
    val proofOfWork: ProofOfWorkVO
)

