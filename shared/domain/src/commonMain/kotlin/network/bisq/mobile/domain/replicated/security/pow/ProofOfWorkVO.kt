package network.bisq.mobile.domain.replicated.security.pow

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.utils.hexToByteArray

@Serializable
data class ProofOfWorkVO(
    val payload: String,
    val counter: Long,
    val challenge: String?,
    val difficulty: Double,
    val solution: String,
    val duration: Long
)

val ProofOfWorkVO.solutionAsByteArray: ByteArray get() = solution.hexToByteArray()
