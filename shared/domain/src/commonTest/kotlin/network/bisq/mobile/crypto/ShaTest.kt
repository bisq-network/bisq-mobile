package network.bisq.mobile.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class ShaTest {
    @Test
    fun sha256_known_vector_abc() {
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val actual = getSha256("abc".encodeToByteArray()).toHexString()
        assertEquals(expected, actual)
    }
}
