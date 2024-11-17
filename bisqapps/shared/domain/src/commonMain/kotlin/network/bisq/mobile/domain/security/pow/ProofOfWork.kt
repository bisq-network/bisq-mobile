package network.bisq.mobile.domain.security.pow

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ProofOfWork(
    @Contextual val payload: ByteArray,
    val counter: Long,
    @Contextual val challenge: ByteArray? = null,
    val difficulty: Double,
    @Contextual val solution: ByteArray,
    val duration: Long
)
