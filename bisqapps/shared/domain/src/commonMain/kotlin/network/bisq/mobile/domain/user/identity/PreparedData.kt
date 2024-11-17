package network.bisq.mobile.domain.user.identity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.security.keys.KeyPair
import network.bisq.mobile.domain.security.pow.ProofOfWork

@Serializable
data class PreparedData(
    @SerialName("keyPair")
    val keyPair: KeyPair,
    val id: String,
    val nym: String,
    val proofOfWork: ProofOfWork
)

