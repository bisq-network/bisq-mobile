package network.bisq.mobile.client.common.domain.utils

class BinaryDecodingUtils(
    private val data: ByteArray,
) {
    private var offset: Int = 0

    private fun requireAvailable(bytes: Int) {
        if (offset + bytes > data.size) {
            throw IllegalStateException("Unexpected end of input")
        }
    }

    fun readByte(): Byte {
        requireAvailable(1)
        return data[offset++]
    }

    fun readUnsignedShort(): Int {
        requireAvailable(2)
        val value =
            ((data[offset].toInt() and 0xFF) shl 8) or
                (data[offset + 1].toInt() and 0xFF)
        offset += 2
        return value
    }

    fun readInt(): Int {
        requireAvailable(4)
        val value =
            ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
        offset += 4
        return value
    }

    fun readLong(): Long {
        requireAvailable(8)
        var result = 0L
        repeat(8) {
            result = (result shl 8) or (data[offset++].toLong() and 0xFF)
        }
        return result
    }

    fun readBytes(): ByteArray = readBytes(Int.MAX_VALUE)

    fun readBytes(maxLength: Int): ByteArray {
        val length = readUnsignedShort()
        if (length > maxLength) {
            throw IllegalArgumentException("Byte array exceeds max length: $length")
        }

        requireAvailable(length)
        val result = data.copyOfRange(offset, offset + length)
        offset += length
        return result
    }

    fun readString(): String = readString(Int.MAX_VALUE)

    fun readString(maxLength: Int): String {
        val bytes = readBytes(maxLength)
        return bytes.decodeToString()
    }
}
