package network.bisq.mobile.data.replicated.settings

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the wire compatibility of the /settings/version payload: nodes that predate
 * imageVersion (or run outside a container) omit the field, and that must keep decoding.
 */
class ApiVersionSettingsVOTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a payload without imageVersion`() {
        val decoded = json.decodeFromString<ApiVersionSettingsVO>("""{"version":"2.1.12"}""")

        assertEquals("2.1.12", decoded.version)
        assertNull(decoded.imageVersion)
    }

    @Test
    fun `decodes a containerised node's payload with imageVersion`() {
        val decoded = json.decodeFromString<ApiVersionSettingsVO>("""{"version":"2.1.12","imageVersion":"2.1.12.2"}""")

        assertEquals("2.1.12", decoded.version)
        assertEquals("2.1.12.2", decoded.imageVersion)
    }
}
