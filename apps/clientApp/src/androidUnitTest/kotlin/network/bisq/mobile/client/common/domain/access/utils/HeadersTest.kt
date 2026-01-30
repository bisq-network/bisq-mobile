package network.bisq.mobile.client.common.domain.access.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class HeadersTest {
    @Test
    fun `CLIENT_ID header name is correct`() {
        assertEquals("Bisq-Client-Id", Headers.CLIENT_ID)
    }

    @Test
    fun `SESSION_ID header name is correct`() {
        assertEquals("Bisq-Session-Id", Headers.SESSION_ID)
    }
}
