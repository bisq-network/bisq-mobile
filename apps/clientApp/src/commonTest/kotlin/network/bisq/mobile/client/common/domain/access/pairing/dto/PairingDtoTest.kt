package network.bisq.mobile.client.common.domain.access.pairing.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PairingRequestDto properties are accessible`() {
        val dto =
            PairingRequestDto(
                version = 1,
                pairingCodeId = "code-123",
                clientName = "MyClient",
            )

        assertEquals(1.toByte(), dto.version)
        assertEquals("code-123", dto.pairingCodeId)
        assertEquals("MyClient", dto.clientName)
    }

    @Test
    fun `PairingRequestDto serialization works`() {
        val dto =
            PairingRequestDto(
                version = 1,
                pairingCodeId = "code-123",
                clientName = "MyClient",
            )

        val jsonString = json.encodeToString(dto)
        val decoded = json.decodeFromString<PairingRequestDto>(jsonString)

        assertEquals(dto, decoded)
    }

    @Test
    fun `PairingRequestDto equality works`() {
        val dto1 = PairingRequestDto(1, "code", "client")
        val dto2 = PairingRequestDto(1, "code", "client")
        assertEquals(dto1, dto2)
    }

    @Test
    fun `PairingResponseDto properties are accessible`() {
        val dto =
            PairingResponseDto(
                version = 1,
                clientId = "client-123",
                clientSecret = "secret-456",
                sessionId = "session-789",
                sessionExpiryDate = 1700000000000L,
            )

        assertEquals(1.toByte(), dto.version)
        assertEquals("client-123", dto.clientId)
        assertEquals("secret-456", dto.clientSecret)
        assertEquals("session-789", dto.sessionId)
        assertEquals(1700000000000L, dto.sessionExpiryDate)
    }

    @Test
    fun `PairingResponseDto serialization works`() {
        val dto =
            PairingResponseDto(
                version = 1,
                clientId = "client-123",
                clientSecret = "secret-456",
                sessionId = "session-789",
                sessionExpiryDate = 1700000000000L,
            )

        val jsonString = json.encodeToString(dto)
        val decoded = json.decodeFromString<PairingResponseDto>(jsonString)

        assertEquals(dto, decoded)
    }

    @Test
    fun `PairingResponseDto equality works`() {
        val dto1 = PairingResponseDto(1, "c", "s", "sess", 1000L)
        val dto2 = PairingResponseDto(1, "c", "s", "sess", 1000L)
        assertEquals(dto1, dto2)
    }

    @Test
    fun `PairingRequestDto copy works`() {
        val original = PairingRequestDto(1, "code", "client")
        val copied = original.copy(clientName = "newClient")
        assertEquals("newClient", copied.clientName)
        assertEquals("code", copied.pairingCodeId)
    }

    @Test
    fun `PairingResponseDto copy works`() {
        val original = PairingResponseDto(1, "c", "s", "sess", 1000L)
        val copied = original.copy(sessionId = "newSess")
        assertEquals("newSess", copied.sessionId)
        assertEquals("c", copied.clientId)
    }
}
