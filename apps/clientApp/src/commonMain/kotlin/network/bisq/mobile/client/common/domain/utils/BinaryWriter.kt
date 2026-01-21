package network.bisq.mobile.client.common.domain.utils

class BinaryWriter(
    initialCapacity: Int = 256,
) {
    private val buffer = ArrayList<Byte>(initialCapacity)

    fun toByteArray(): ByteArray = buffer.toByteArray()

    fun writeByte(value: Byte) {
        buffer.add(value)
    }

    fun writeShort(value: Int) {
        buffer.add(((value ushr 8) and 0xFF).toByte())
        buffer.add((value and 0xFF).toByte())
    }

    fun writeInt(value: Int) {
        buffer.add(((value ushr 24) and 0xFF).toByte())
        buffer.add(((value ushr 16) and 0xFF).toByte())
        buffer.add(((value ushr 8) and 0xFF).toByte())
        buffer.add((value and 0xFF).toByte())
    }

    fun writeLong(value: Long) {
        for (i in 7 downTo 0) {
            buffer.add(((value ushr (i * 8)) and 0xFF).toByte())
        }
    }

    fun writeBytes(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
    }
}
