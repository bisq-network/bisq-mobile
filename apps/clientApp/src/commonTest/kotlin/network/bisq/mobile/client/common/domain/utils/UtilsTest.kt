package network.bisq.mobile.client.common.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun `EMPTY_STRING is empty`() {
        assertEquals("", EMPTY_STRING)
    }

    @Test
    fun `EMPTY_STRING has zero length`() {
        assertEquals(0, EMPTY_STRING.length)
    }

    @Test
    fun `EMPTY_STRING is blank`() {
        assertEquals(true, EMPTY_STRING.isBlank())
    }

    @Test
    fun `EMPTY_STRING is empty check`() {
        assertEquals(true, EMPTY_STRING.isEmpty())
    }
}
