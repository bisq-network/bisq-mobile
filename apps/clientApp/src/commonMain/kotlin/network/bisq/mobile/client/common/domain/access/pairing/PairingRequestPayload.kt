package network.bisq.mobile.client.common.domain.access.pairing

import kotlinx.datetime.Instant

data class PairingRequestPayload(
    val pairingCodeId: String,
    val clientPublicKey: ByteArray,
    val deviceName: String,
    val timestamp: Instant
) {
    companion object {
        const val VERSION: Byte = 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PairingRequestPayload

        if (pairingCodeId != other.pairingCodeId) return false
        if (!clientPublicKey.contentEquals(other.clientPublicKey)) return false
        if (deviceName != other.deviceName) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pairingCodeId.hashCode()
        result = 31 * result + clientPublicKey.contentHashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
