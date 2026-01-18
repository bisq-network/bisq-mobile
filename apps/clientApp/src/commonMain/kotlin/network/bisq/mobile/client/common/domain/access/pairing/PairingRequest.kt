package network.bisq.mobile.client.common.domain.access.pairing

class PairingRequest(
    val payload: PairingRequestPayload,
    signature: ByteArray,
) {
    private val _signature: ByteArray = signature.copyOf()

    val signature: ByteArray
        get() = _signature.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PairingRequest) return false

        if (payload != other.payload) return false
        if (!_signature.contentEqualsNullable(other._signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = payload.hashCode()
        result = 31 * result + (_signature.contentHashCode())
        return result
    }

    private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
        when {
            this === other -> true
            this == null || other == null -> false
            else -> this.contentEquals(other)
        }
}
