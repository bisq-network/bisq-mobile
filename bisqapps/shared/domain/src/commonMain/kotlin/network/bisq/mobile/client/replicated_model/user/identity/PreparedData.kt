package network.bisq.mobile.client.replicated_model.user.identity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.security.keys.KeyPair
import network.bisq.mobile.client.replicated_model.security.pow.ProofOfWork

@Serializable
data class PreparedData(
    @SerialName("keyPair")
    val keyPair: KeyPair,
    val id: String,
    val nym: String,
    val proofOfWork: ProofOfWork
)

