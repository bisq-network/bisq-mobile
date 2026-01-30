package network.bisq.mobile.client.common.domain.access.pairing

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PairingCodeTest {
    @Test
    fun `VERSION constant is 1`() {
        assertEquals(1.toByte(), PairingCode.VERSION)
    }

    @Test
    fun `data class properties are accessible`() {
        val expiresAt = Instant.fromEpochMilliseconds(1700000000000L)
        val permissions = setOf(Permission.OFFERBOOK, Permission.TRADES)

        val pairingCode =
            PairingCode(
                id = "test-id",
                expiresAt = expiresAt,
                grantedPermissions = permissions,
            )

        assertEquals("test-id", pairingCode.id)
        assertEquals(expiresAt, pairingCode.expiresAt)
        assertEquals(permissions, pairingCode.grantedPermissions)
    }

    @Test
    fun `data class equality works correctly`() {
        val expiresAt = Instant.fromEpochMilliseconds(1700000000000L)
        val permissions = setOf(Permission.OFFERBOOK)

        val code1 = PairingCode("id1", expiresAt, permissions)
        val code2 = PairingCode("id1", expiresAt, permissions)

        assertEquals(code1, code2)
    }

    @Test
    fun `data class copy works correctly`() {
        val expiresAt = Instant.fromEpochMilliseconds(1700000000000L)
        val permissions = setOf(Permission.OFFERBOOK)

        val original = PairingCode("id1", expiresAt, permissions)
        val copied = original.copy(id = "id2")

        assertEquals("id2", copied.id)
        assertEquals(expiresAt, copied.expiresAt)
        assertEquals(permissions, copied.grantedPermissions)
    }

    @Test
    fun `empty permissions set is allowed`() {
        val expiresAt = Instant.fromEpochMilliseconds(1700000000000L)
        val pairingCode = PairingCode("id", expiresAt, emptySet())

        assertTrue(pairingCode.grantedPermissions.isEmpty())
    }

    @Test
    fun `all permissions can be granted`() {
        val expiresAt = Instant.fromEpochMilliseconds(1700000000000L)
        val allPermissions = Permission.entries.toSet()

        val pairingCode = PairingCode("id", expiresAt, allPermissions)

        assertEquals(Permission.entries.size, pairingCode.grantedPermissions.size)
    }
}
