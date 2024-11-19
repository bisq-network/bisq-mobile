package network.bisq.mobile.client.replicated_model.security.pow

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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProofOfWork

        if (!payload.contentEquals(other.payload)) return false
        if (counter != other.counter) return false
        if (challenge != null) {
            if (other.challenge == null) return false
            if (!challenge.contentEquals(other.challenge)) return false
        } else if (other.challenge != null) return false
        if (difficulty != other.difficulty) return false
        if (!solution.contentEquals(other.solution)) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + (challenge?.contentHashCode() ?: 0)
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + duration.hashCode()
        return result
    }
}
