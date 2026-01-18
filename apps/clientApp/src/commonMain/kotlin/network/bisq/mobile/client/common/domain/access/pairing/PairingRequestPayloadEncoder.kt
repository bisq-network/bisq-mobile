package network.bisq.mobile.client.common.domain.access.pairing

import network.bisq.mobile.client.common.domain.utils.BinaryEncodingUtils
import network.bisq.mobile.client.common.domain.utils.BinaryWriter

object PairingRequestPayloadEncoder {

    private const val MAX_PAIRING_CODE_ID_LENGTH = 36
    private const val MAX_PUBLIC_KEY_BYTES = 128
    private const val MAX_DEVICE_NAME_LENGTH = 128

    fun encode(payload: PairingRequestPayload): ByteArray {
        require(payload.pairingCodeId.length == 36) {
            "PairingCodeId is expected to be 36 chars long"
        }
        require(payload.deviceName.length >= 4) {
            "DeviceName must have at least 4 chars"
        }
        require(payload.deviceName.length <= 32) {
            "DeviceName must not be longer than 32 chars"
        }

        val writer = BinaryWriter()

        BinaryEncodingUtils.writeByte(writer, PairingRequestPayload.VERSION)
        BinaryEncodingUtils.writeString(writer, payload.pairingCodeId, MAX_PAIRING_CODE_ID_LENGTH)
        BinaryEncodingUtils.writeBytes(writer, payload.clientPublicKey, MAX_PUBLIC_KEY_BYTES)
        BinaryEncodingUtils.writeString(writer, payload.deviceName, MAX_DEVICE_NAME_LENGTH)
        BinaryEncodingUtils.writeLong(writer, payload.timestamp.toEpochMilliseconds())

        return writer.toByteArray()
    }
}
