package network.bisq.mobile.client.common.domain.access.pairing

import kotlinx.datetime.Instant

data class PairingRequestPayload(
    val version: Byte,
    val pairingCodeId: String,
    val clientPublicKey: ByteArray,
    val deviceName: String,
    val timestamp: Instant,
) {
    companion object {
        const val VERSION: Byte = 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PairingRequestPayload) return false

        return version == other.version &&
                pairingCodeId == other.pairingCodeId &&
                clientPublicKey.contentEquals(other.clientPublicKey) &&
                deviceName == other.deviceName &&
                timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + pairingCodeId.hashCode()
        result = 31 * result + clientPublicKey.contentHashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
