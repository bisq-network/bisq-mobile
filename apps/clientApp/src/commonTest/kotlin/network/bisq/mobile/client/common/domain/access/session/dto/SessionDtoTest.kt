package network.bisq.mobile.client.common.domain.access.session.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SessionRequestDto properties are accessible`() {
        val dto =
            SessionRequestDto(
                clientId = "client-123",
                clientSecret = "secret-456",
            )

        assertEquals("client-123", dto.clientId)
        assertEquals("secret-456", dto.clientSecret)
    }

    @Test
    fun `SessionRequestDto serialization works`() {
        val dto =
            SessionRequestDto(
                clientId = "client-123",
                clientSecret = "secret-456",
            )

        val jsonString = json.encodeToString(dto)
        val decoded = json.decodeFromString<SessionRequestDto>(jsonString)

        assertEquals(dto, decoded)
    }

    @Test
    fun `SessionRequestDto equality works`() {
        val dto1 = SessionRequestDto("client", "secret")
        val dto2 = SessionRequestDto("client", "secret")
        assertEquals(dto1, dto2)
    }

    @Test
    fun `SessionRequestDto copy works`() {
        val original = SessionRequestDto("client", "secret")
        val copied = original.copy(clientId = "newClient")
        assertEquals("newClient", copied.clientId)
        assertEquals("secret", copied.clientSecret)
    }

    @Test
    fun `SessionResponseDto properties are accessible`() {
        val dto =
            SessionResponseDto(
                sessionId = "session-123",
                expiresAt = 1700000000000L,
            )

        assertEquals("session-123", dto.sessionId)
        assertEquals(1700000000000L, dto.expiresAt)
    }

    @Test
    fun `SessionResponseDto serialization works`() {
        val dto =
            SessionResponseDto(
                sessionId = "session-123",
                expiresAt = 1700000000000L,
            )

        val jsonString = json.encodeToString(dto)
        val decoded = json.decodeFromString<SessionResponseDto>(jsonString)

        assertEquals(dto, decoded)
    }

    @Test
    fun `SessionResponseDto equality works`() {
        val dto1 = SessionResponseDto("sess", 1000L)
        val dto2 = SessionResponseDto("sess", 1000L)
        assertEquals(dto1, dto2)
    }

    @Test
    fun `SessionResponseDto copy works`() {
        val original = SessionResponseDto("sess", 1000L)
        val copied = original.copy(expiresAt = 2000L)
        assertEquals("sess", copied.sessionId)
        assertEquals(2000L, copied.expiresAt)
    }
}
