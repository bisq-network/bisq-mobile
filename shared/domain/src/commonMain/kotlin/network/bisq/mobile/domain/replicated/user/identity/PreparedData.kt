package network.bisq.mobile.domain.replicated.user.identity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.security.keys.KeyPair

@Serializable
data class PreparedData(
    @SerialName("keyPair")
    val keyPair: KeyPair,
    val id: String,
    val nym: String,
    val proofOfWork: network.bisq.mobile.domain.replicated.security.pow.ProofOfWork
)

