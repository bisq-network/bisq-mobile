package network.bisq.mobile.domain.data.replicated.security.pow

import kotlinx.serialization.Serializable

@Serializable
data class ProofOfWorkVO(
    val payloadEncoded: String,
    val counter: Long,
    val challengeEncoded: String?,
    val difficulty: Double,
    val solutionEncoded: String,
    val duration: Long
)
