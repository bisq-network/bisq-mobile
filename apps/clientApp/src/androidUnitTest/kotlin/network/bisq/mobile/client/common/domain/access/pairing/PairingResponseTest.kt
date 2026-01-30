package network.bisq.mobile.client.common.domain.access.pairing

import kotlin.test.Test
import kotlin.test.assertEquals

class PairingResponseTest {
    @Test
    fun `data class properties are accessible`() {
        val response =
            PairingResponse(
                version = 1,
                clientId = "client-123",
                clientSecret = "secret-456",
                sessionId = "session-789",
                sessionExpiryDate = 1700000000000L,
            )

        assertEquals(1.toByte(), response.version)
        assertEquals("client-123", response.clientId)
        assertEquals("secret-456", response.clientSecret)
        assertEquals("session-789", response.sessionId)
        assertEquals(1700000000000L, response.sessionExpiryDate)
    }

    @Test
    fun `data class equality works correctly`() {
        val response1 = PairingResponse(1, "c1", "s1", "sess1", 1000L)
        val response2 = PairingResponse(1, "c1", "s1", "sess1", 1000L)

        assertEquals(response1, response2)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = PairingResponse(1, "c1", "s1", "sess1", 1000L)
        val copied = original.copy(clientId = "c2")

        assertEquals("c2", copied.clientId)
        assertEquals("s1", copied.clientSecret)
        assertEquals("sess1", copied.sessionId)
    }
}
