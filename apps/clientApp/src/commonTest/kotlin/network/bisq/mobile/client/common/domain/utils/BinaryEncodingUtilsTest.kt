package network.bisq.mobile.client.common.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BinaryEncodingUtilsTest {
    @Test
    fun `writeString writes length-prefixed string`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeString(writer, "Hi")

        val result = writer.toByteArray()
        // Length prefix (2 bytes) + "Hi" (2 bytes)
        assertEquals(4, result.size)
        assertEquals(0x00.toByte(), result[0]) // high byte of length
        assertEquals(0x02.toByte(), result[1]) // low byte of length
        assertEquals('H'.code.toByte(), result[2])
        assertEquals('i'.code.toByte(), result[3])
    }

    @Test
    fun `writeString with maxLength enforces limit`() {
        val writer = BinaryWriter()
        assertFailsWith<IllegalArgumentException> {
            BinaryEncodingUtils.writeString(writer, "Hello", 3)
        }
    }

    @Test
    fun `writeString with maxLength allows within limit`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeString(writer, "Hi", 10)
        assertEquals(4, writer.toByteArray().size)
    }

    @Test
    fun `writeInt writes four bytes`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeInt(writer, 0x01020304)
        assertEquals(
            listOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte()),
            writer.toByteArray().toList(),
        )
    }

    @Test
    fun `writeLong writes eight bytes`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeLong(writer, 0x0102030405060708L)
        assertEquals(8, writer.toByteArray().size)
    }

    @Test
    fun `writeByte writes single byte`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, 0x42)
        assertEquals(listOf(0x42.toByte()), writer.toByteArray().toList())
    }

    @Test
    fun `writeBytes writes length-prefixed bytes`() {
        val writer = BinaryWriter()
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        BinaryEncodingUtils.writeBytes(writer, bytes)

        val result = writer.toByteArray()
        // Length prefix (2 bytes) + bytes (3 bytes)
        assertEquals(5, result.size)
        assertEquals(0x00.toByte(), result[0])
        assertEquals(0x03.toByte(), result[1])
        assertEquals(0x01.toByte(), result[2])
        assertEquals(0x02.toByte(), result[3])
        assertEquals(0x03.toByte(), result[4])
    }

    @Test
    fun `writeBytes with maxLength enforces limit`() {
        val writer = BinaryWriter()
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        assertFailsWith<IllegalArgumentException> {
            BinaryEncodingUtils.writeBytes(writer, bytes, 3)
        }
    }

    @Test
    fun `writeBytes with maxLength allows within limit`() {
        val writer = BinaryWriter()
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        BinaryEncodingUtils.writeBytes(writer, bytes, 10)
        assertEquals(5, writer.toByteArray().size)
    }

    @Test
    fun `writeBytes rejects array exceeding 16-bit length`() {
        val writer = BinaryWriter()
        // Create array larger than 65535 bytes
        val largeBytes = ByteArray(65536)
        assertFailsWith<IllegalArgumentException> {
            BinaryEncodingUtils.writeBytes(writer, largeBytes)
        }
    }

    @Test
    fun `writeBytes with maxLength rejects maxLength exceeding 65535`() {
        val writer = BinaryWriter()
        val bytes = byteArrayOf(0x01)
        assertFailsWith<IllegalArgumentException> {
            BinaryEncodingUtils.writeBytes(writer, bytes, 65536)
        }
    }

    @Test
    fun `empty string is written correctly`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeString(writer, "")

        val result = writer.toByteArray()
        assertEquals(2, result.size) // Just length prefix
        assertEquals(0x00.toByte(), result[0])
        assertEquals(0x00.toByte(), result[1])
    }

    @Test
    fun `empty bytes array is written correctly`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeBytes(writer, byteArrayOf())

        val result = writer.toByteArray()
        assertEquals(2, result.size) // Just length prefix
        assertEquals(0x00.toByte(), result[0])
        assertEquals(0x00.toByte(), result[1])
    }
}
