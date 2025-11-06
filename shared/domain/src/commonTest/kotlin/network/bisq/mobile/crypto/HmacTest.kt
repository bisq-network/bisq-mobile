package network.bisq.mobile.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class HmacTest {
    @Test
    fun rfc4231_case1() {
        // Key = 20 bytes of 0x0b, Data = "Hi There"
        val key = ByteArray(20) { 0x0b.toByte() }
        val data = "Hi There".encodeToByteArray()
        val expected = "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"
        assertEquals(expected, hmacSha256(key, data).toHexString())
    }

    @Test
    fun rfc4231_case2() {
        // Key = "Jefe", Data = "what do ya want for nothing?"
        val key = "Jefe".encodeToByteArray()
        val data = "what do ya want for nothing?".encodeToByteArray()
        val expected = "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"
        assertEquals(expected, hmacSha256(key, data).toHexString())
    }
}