package network.bisq.mobile.client.common.domain.utils

object BinaryEncodingUtils {
    private const val MAX_USHORT = 0xFFFF

    fun writeString(
        writer: BinaryWriter,
        value: String,
        maxByteLength: Int,
    ) {
        val bytes = value.encodeToByteArray()
        writeBytes(writer, bytes, maxByteLength)
    }

    fun writeString(
        writer: BinaryWriter,
        value: String,
    ) {
        val bytes = value.encodeToByteArray()
        writeBytes(writer, bytes)
    }

    fun writeInt(
        writer: BinaryWriter,
        value: Int,
    ) {
        writer.writeInt(value)
    }

    fun writeLong(
        writer: BinaryWriter,
        value: Long,
    ) {
        writer.writeLong(value)
    }

    fun writeByte(
        writer: BinaryWriter,
        value: Byte,
    ) {
        writer.writeByte(value)
    }

    fun writeBytes(
        writer: BinaryWriter,
        value: ByteArray,
        maxLength: Int,
    ) {
        require(maxLength <= MAX_USHORT) {
            "Max length exceeds 65535."
        }
        require(value.size <= maxLength) {
            "Byte array too long."
        }
        writeBytes(writer, value)
    }

    fun writeBytes(
        writer: BinaryWriter,
        value: ByteArray,
    ) {
        require(value.size <= MAX_USHORT) {
            "Byte array too long for 16-bit length prefix."
        }
        writer.writeShort(value.size)
        writer.writeBytes(value)
    }
}
