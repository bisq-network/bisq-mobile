package network.bisq.mobile.client.common.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class BinaryWriterTest {
    @Test
    fun `writeByte writes single byte`() {
        val writer = BinaryWriter()
        writer.writeByte(0x42)
        assertEquals(listOf(0x42.toByte()), writer.toByteArray().toList())
    }

    @Test
    fun `writeShort writes two bytes in big-endian order`() {
        val writer = BinaryWriter()
        writer.writeShort(0x0102) // 258
        assertEquals(listOf(0x01.toByte(), 0x02.toByte()), writer.toByteArray().toList())
    }

    @Test
    fun `writeShort handles max unsigned short value`() {
        val writer = BinaryWriter()
        writer.writeShort(0xFFFF) // 65535
        assertEquals(listOf(0xFF.toByte(), 0xFF.toByte()), writer.toByteArray().toList())
    }

    @Test
    fun `writeInt writes four bytes in big-endian order`() {
        val writer = BinaryWriter()
        writer.writeInt(0x01020304) // 16909060
        assertEquals(
            listOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte()),
            writer.toByteArray().toList(),
        )
    }

    @Test
    fun `writeInt handles negative values`() {
        val writer = BinaryWriter()
        writer.writeInt(-1) // 0xFFFFFFFF
        assertEquals(
            listOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            writer.toByteArray().toList(),
        )
    }

    @Test
    fun `writeLong writes eight bytes in big-endian order`() {
        val writer = BinaryWriter()
        writer.writeLong(0x0102030405060708L)
        assertEquals(
            listOf(
                0x01.toByte(),
                0x02.toByte(),
                0x03.toByte(),
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0x07.toByte(),
                0x08.toByte(),
            ),
            writer.toByteArray().toList(),
        )
    }

    @Test
    fun `writeLong handles negative values`() {
        val writer = BinaryWriter()
        writer.writeLong(-1L) // 0xFFFFFFFFFFFFFFFF
        assertEquals(
            List(8) { 0xFF.toByte() },
            writer.toByteArray().toList(),
        )
    }

    @Test
    fun `writeBytes writes byte array`() {
        val writer = BinaryWriter()
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        writer.writeBytes(bytes)
        assertEquals(bytes.toList(), writer.toByteArray().toList())
    }

    @Test
    fun `sequential writes accumulate correctly`() {
        val writer = BinaryWriter()
        writer.writeByte(0x42)
        writer.writeShort(0x0102)
        writer.writeInt(0x03040506)

        val expected =
            listOf(
                0x42.toByte(), // byte
                0x01.toByte(),
                0x02.toByte(), // short
                0x03.toByte(),
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(), // int
            )
        assertEquals(expected, writer.toByteArray().toList())
    }

    @Test
    fun `empty writer returns empty array`() {
        val writer = BinaryWriter()
        assertEquals(emptyList(), writer.toByteArray().toList())
    }

    @Test
    fun `writeBytes with empty array works`() {
        val writer = BinaryWriter()
        writer.writeBytes(byteArrayOf())
        assertEquals(emptyList(), writer.toByteArray().toList())
    }
}
