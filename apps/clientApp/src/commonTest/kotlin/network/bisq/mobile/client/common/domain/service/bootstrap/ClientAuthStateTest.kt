package network.bisq.mobile.client.common.domain.service.bootstrap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientAuthStateTest {
    @Test
    fun `REQUIRE_PAIRING exists`() {
        val state = ClientAuthState.REQUIRE_PAIRING
        assertEquals("REQUIRE_PAIRING", state.name)
    }

    @Test
    fun `RENEW_SESSION exists`() {
        val state = ClientAuthState.RENEW_SESSION
        assertEquals("RENEW_SESSION", state.name)
    }

    @Test
    fun `all enum values are defined`() {
        val values = ClientAuthState.entries
        assertEquals(2, values.size)
        assertTrue(values.contains(ClientAuthState.REQUIRE_PAIRING))
        assertTrue(values.contains(ClientAuthState.RENEW_SESSION))
    }

    @Test
    fun `enum values have correct ordinals`() {
        assertEquals(0, ClientAuthState.REQUIRE_PAIRING.ordinal)
        assertEquals(1, ClientAuthState.RENEW_SESSION.ordinal)
    }

    @Test
    fun `valueOf returns correct enum`() {
        assertEquals(ClientAuthState.REQUIRE_PAIRING, ClientAuthState.valueOf("REQUIRE_PAIRING"))
        assertEquals(ClientAuthState.RENEW_SESSION, ClientAuthState.valueOf("RENEW_SESSION"))
    }
}
