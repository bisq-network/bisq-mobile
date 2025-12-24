package network.bisq.mobile.domain.utils

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ByteArrayUtilTest {
    // Base64 encoding/decoding tests
    @Test
    fun `base64ToByteArray decodes valid base64 string`() {
        // "hello" in base64 is "aGVsbG8="
        val result = "aGVsbG8=".base64ToByteArray()
        assertContentEquals("hello".encodeToByteArray(), result)
    }

    @Test
    fun `base64ToByteArray decodes empty string to empty array`() {
        val result = "".base64ToByteArray()
        assertContentEquals(byteArrayOf(), result)
    }

    @Test
    fun `base64ToByteArray returns null for invalid base64`() {
        val result = "not-valid-base64!!!".base64ToByteArray()
        assertNull(result)
    }

    @Test
    fun `base64ToByteArray handles binary data`() {
        // Base64 for bytes [0, 127, 255] -> "AH//", but 255 as signed byte is -1
        val result = "AH//".base64ToByteArray()
        assertContentEquals(byteArrayOf(0, 127, -1), result)
    }

    // hexToByteArray tests
    @Test
    fun `hexToByteArray converts valid hex string`() {
        val result = "48656c6c6f".hexToByteArray() // "Hello"
        assertContentEquals("Hello".encodeToByteArray(), result)
    }

    @Test
    fun `hexToByteArray handles uppercase hex`() {
        val result = "48656C6C6F".hexToByteArray() // "Hello"
        assertContentEquals("Hello".encodeToByteArray(), result)
    }

    @Test
    fun `hexToByteArray handles empty string`() {
        val result = "".hexToByteArray()
        assertContentEquals(byteArrayOf(), result)
    }

    @Test
    fun `hexToByteArray handles all byte values`() {
        val result = "00ff7f80".hexToByteArray()
        assertContentEquals(byteArrayOf(0, -1, 127, -128), result)
    }

    @Test
    fun `hexToByteArray throws for odd length string`() {
        assertFailsWith<IllegalArgumentException> {
            "abc".hexToByteArray()
        }
    }

    // concat tests
    @Test
    fun `concat joins multiple byte arrays`() {
        val a = byteArrayOf(1, 2)
        val b = byteArrayOf(3, 4, 5)
        val c = byteArrayOf(6)
        val result = concat(a, b, c)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), result)
    }

    @Test
    fun `concat handles empty arrays`() {
        val a = byteArrayOf(1, 2)
        val empty = byteArrayOf()
        val b = byteArrayOf(3)
        val result = concat(a, empty, b)
        assertContentEquals(byteArrayOf(1, 2, 3), result)
    }

    @Test
    fun `concat with single array returns copy`() {
        val a = byteArrayOf(1, 2, 3)
        val result = concat(a)
        assertContentEquals(a, result)
    }

    @Test
    fun `concat with no arrays returns empty array`() {
        val result = concat()
        assertContentEquals(byteArrayOf(), result)
    }

    // ByteArrayAsBase64Serializer tests
    @Test
    fun `ByteArrayAsBase64Serializer has correct descriptor`() {
        assertEquals("ByteArrayAsBase64", ByteArrayAsBase64Serializer.descriptor.serialName)
    }

    @Test
    fun `ByteArrayAsBase64Serializer round-trip preserves data`() {
        val original = byteArrayOf(0, 1, 127, -128, -1)
        val json = Json.encodeToString(ByteArrayAsBase64Serializer, original)
        val decoded = Json.decodeFromString(ByteArrayAsBase64Serializer, json)
        assertContentEquals(original, decoded)
    }
}
