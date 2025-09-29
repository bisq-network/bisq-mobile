package network.bisq.mobile.domain.data.replicated.security.pow

import kotlinx.serialization.Serializable

@Serializable
data class ProofOfWorkVO(
    val payloadEncoded: String, // Base64 encoded
    val counter: Long,
    val challengeEncoded: String?, // Base64 encoded
    val difficulty: Double,
    val solutionEncoded: String, // Base64 encoded
    val duration: Long
)
