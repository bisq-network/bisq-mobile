package network.bisq.mobile.client.common.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BinaryDecodingUtilsTest {
    @Test
    fun `readByte returns correct byte value`() {
        val data = byteArrayOf(0x42)
        val reader = BinaryDecodingUtils(data)
        assertEquals(0x42.toByte(), reader.readByte())
    }

    @Test
    fun `readByte throws when no data available`() {
        val data = byteArrayOf()
        val reader = BinaryDecodingUtils(data)
        assertFailsWith<IllegalStateException> {
            reader.readByte()
        }
    }

    @Test
    fun `readUnsignedShort returns correct value`() {
        // 0x0102 = 258
        val data = byteArrayOf(0x01, 0x02)
        val reader = BinaryDecodingUtils(data)
        assertEquals(258, reader.readUnsignedShort())
    }

    @Test
    fun `readUnsignedShort handles max value`() {
        // 0xFFFF = 65535
        val data = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        val reader = BinaryDecodingUtils(data)
        assertEquals(65535, reader.readUnsignedShort())
    }

    @Test
    fun `readUnsignedShort throws when insufficient data`() {
        val data = byteArrayOf(0x01)
        val reader = BinaryDecodingUtils(data)
        assertFailsWith<IllegalStateException> {
            reader.readUnsignedShort()
        }
    }

    @Test
    fun `readInt returns correct value`() {
        // 0x01020304 = 16909060
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val reader = BinaryDecodingUtils(data)
        assertEquals(16909060, reader.readInt())
    }

    @Test
    fun `readInt handles negative values`() {
        // 0xFFFFFFFF = -1
        val data = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val reader = BinaryDecodingUtils(data)
        assertEquals(-1, reader.readInt())
    }

    @Test
    fun `readInt throws when insufficient data`() {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val reader = BinaryDecodingUtils(data)
        assertFailsWith<IllegalStateException> {
            reader.readInt()
        }
    }

    @Test
    fun `readLong returns correct value`() {
        // 0x0102030405060708
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val reader = BinaryDecodingUtils(data)
        assertEquals(0x0102030405060708L, reader.readLong())
    }

    @Test
    fun `readLong handles negative values`() {
        val data = ByteArray(8) { 0xFF.toByte() }
        val reader = BinaryDecodingUtils(data)
        assertEquals(-1L, reader.readLong())
    }

    @Test
    fun `readLong throws when insufficient data`() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        val reader = BinaryDecodingUtils(data)
        assertFailsWith<IllegalStateException> {
            reader.readLong()
        }
    }

    @Test
    fun `readString returns correct string`() {
        val testString = "Hello"
        val stringBytes = testString.encodeToByteArray()
        // Length prefix (2 bytes) + string bytes
        val data = byteArrayOf(0x00, stringBytes.size.toByte()) + stringBytes
        val reader = BinaryDecodingUtils(data)
        assertEquals(testString, reader.readString())
    }

    @Test
    fun `readString with maxLength enforces limit`() {
        val testString = "Hello"
        val stringBytes = testString.encodeToByteArray()
        val data = byteArrayOf(0x00, stringBytes.size.toByte()) + stringBytes
        val reader = BinaryDecodingUtils(data)
        // maxLength of 3 should fail since string is 5 bytes
        assertFailsWith<IllegalArgumentException> {
            reader.readString(3)
        }
    }

    @Test
    fun `readBytes returns correct bytes`() {
        val testBytes = byteArrayOf(0x01, 0x02, 0x03)
        // Length prefix (2 bytes) + bytes
        val data = byteArrayOf(0x00, testBytes.size.toByte()) + testBytes
        val reader = BinaryDecodingUtils(data)
        assertEquals(testBytes.toList(), reader.readBytes().toList())
    }

    @Test
    fun `readBytes with maxLength enforces limit`() {
        val testBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val data = byteArrayOf(0x00, testBytes.size.toByte()) + testBytes
        val reader = BinaryDecodingUtils(data)
        assertFailsWith<IllegalArgumentException> {
            reader.readBytes(3)
        }
    }

    @Test
    fun `sequential reads work correctly`() {
        // Build data: byte + short + int
        val data =
            byteArrayOf(
                0x42, // byte
                0x01,
                0x02, // short (258)
                0x01,
                0x02,
                0x03,
                0x04, // int (16909060)
            )
        val reader = BinaryDecodingUtils(data)

        assertEquals(0x42.toByte(), reader.readByte())
        assertEquals(258, reader.readUnsignedShort())
        assertEquals(16909060, reader.readInt())
    }

    @Test
    fun `empty string is read correctly`() {
        val data = byteArrayOf(0x00, 0x00) // length = 0
        val reader = BinaryDecodingUtils(data)
        assertEquals("", reader.readString())
    }
}
