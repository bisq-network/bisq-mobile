package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UUIDTest {
    @Test
    fun `createUuid returns valid UUID format`() {
        val uuid = createUuid()

        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(uuidRegex.matches(uuid), "UUID '$uuid' does not match expected format")
    }

    @Test
    fun `createUuid returns string of correct length`() {
        val uuid = createUuid()
        assertEquals(36, uuid.length)
    }

    @Test
    fun `createUuid returns unique values`() {
        val uuid1 = createUuid()
        val uuid2 = createUuid()
        val uuid3 = createUuid()

        assertNotEquals(uuid1, uuid2)
        assertNotEquals(uuid2, uuid3)
        assertNotEquals(uuid1, uuid3)
    }

    @Test
    fun `createUuid returns lowercase string`() {
        val uuid = createUuid()
        assertEquals(uuid, uuid.lowercase())
    }

    @Test
    fun `createUuid contains correct number of hyphens`() {
        val uuid = createUuid()
        val hyphenCount = uuid.count { it == '-' }
        assertEquals(4, hyphenCount)
    }
}
