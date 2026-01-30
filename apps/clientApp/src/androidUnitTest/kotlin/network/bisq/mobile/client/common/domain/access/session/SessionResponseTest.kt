package network.bisq.mobile.client.common.domain.access.session

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionResponseTest {
    @Test
    fun `data class properties are accessible`() {
        val response =
            SessionResponse(
                sessionId = "session-123",
                expiresAt = 1700000000000L,
            )

        assertEquals("session-123", response.sessionId)
        assertEquals(1700000000000L, response.expiresAt)
    }

    @Test
    fun `data class equality works correctly`() {
        val response1 = SessionResponse("sess1", 1000L)
        val response2 = SessionResponse("sess1", 1000L)

        assertEquals(response1, response2)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = SessionResponse("sess1", 1000L)
        val copied = original.copy(sessionId = "sess2")

        assertEquals("sess2", copied.sessionId)
        assertEquals(1000L, copied.expiresAt)
    }

    @Test
    fun `data class copy can update expiresAt`() {
        val original = SessionResponse("sess1", 1000L)
        val copied = original.copy(expiresAt = 2000L)

        assertEquals("sess1", copied.sessionId)
        assertEquals(2000L, copied.expiresAt)
    }
}
