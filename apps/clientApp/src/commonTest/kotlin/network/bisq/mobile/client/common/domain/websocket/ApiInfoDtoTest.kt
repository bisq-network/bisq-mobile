package network.bisq.mobile.client.common.domain.websocket

import network.bisq.mobile.client.common.di.clientJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the wire shape of `GET /info`; decoded with the production client Json. */
class ApiInfoDtoTest {
    @Test
    fun `decodes a full payload`() {
        val decoded =
            clientJson.decodeFromString<ApiInfoDto>(
                """{"version":"2.1.13","imageVersion":"2.1.13.2","features":["closed-trades","network-info"]}""",
            )

        assertEquals("2.1.13", decoded.version)
        assertEquals("2.1.13.2", decoded.imageVersion)
        assertEquals(listOf("closed-trades", "network-info"), decoded.features)
    }

    @Test
    fun `decodes a payload without imageVersion and with no features`() {
        val decoded = clientJson.decodeFromString<ApiInfoDto>("""{"version":"2.1.13","features":[]}""")

        assertNull(decoded.imageVersion)
        assertEquals(emptyList(), decoded.features)
    }

    @Test
    fun `features default to null when absent`() {
        val decoded = clientJson.decodeFromString<ApiInfoDto>("""{"version":"2.1.13"}""")

        assertNull(decoded.features)
    }
}
