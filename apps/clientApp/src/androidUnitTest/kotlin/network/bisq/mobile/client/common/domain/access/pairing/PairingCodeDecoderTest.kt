package network.bisq.mobile.client.common.domain.access.pairing

import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.utils.BinaryEncodingUtils
import network.bisq.mobile.client.common.domain.utils.BinaryWriter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class PairingCodeDecoderTest {
    private fun encodePairingCode(
        version: Byte = PairingCode.VERSION,
        id: String = "test-id",
        expiresAtMillis: Long = 1700000000000L,
        permissions: Set<Permission> = setOf(Permission.OFFERBOOK),
    ): ByteArray {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, version)
        BinaryEncodingUtils.writeString(writer, id)
        BinaryEncodingUtils.writeLong(writer, expiresAtMillis)
        BinaryEncodingUtils.writeInt(writer, permissions.size)
        permissions.forEach { permission ->
            BinaryEncodingUtils.writeInt(writer, permission.id)
        }
        return writer.toByteArray()
    }

    @Test
    fun `decode bytes returns correct PairingCode`() {
        val bytes =
            encodePairingCode(
                id = "my-pairing-id",
                expiresAtMillis = 1700000000000L,
                permissions = setOf(Permission.OFFERBOOK, Permission.TRADES),
            )

        val result = PairingCodeDecoder.decode(bytes)

        assertEquals("my-pairing-id", result.id)
        assertEquals(Instant.fromEpochMilliseconds(1700000000000L), result.expiresAt)
        assertEquals(2, result.grantedPermissions.size)
        assertTrue(result.grantedPermissions.contains(Permission.OFFERBOOK))
        assertTrue(result.grantedPermissions.contains(Permission.TRADES))
    }

    @Test
    fun `decode base64 returns correct PairingCode`() {
        val bytes =
            encodePairingCode(
                id = "base64-test",
                expiresAtMillis = 1700000000000L,
                permissions = setOf(Permission.SETTINGS),
            )
        val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

        val result = PairingCodeDecoder.decode(base64)

        assertEquals("base64-test", result.id)
        assertTrue(result.grantedPermissions.contains(Permission.SETTINGS))
    }

    @Test
    fun `decode with empty permissions returns empty set`() {
        val bytes =
            encodePairingCode(
                id = "no-perms",
                permissions = emptySet(),
            )

        val result = PairingCodeDecoder.decode(bytes)

        assertTrue(result.grantedPermissions.isEmpty())
    }

    @Test
    fun `decode with all permissions returns all permissions`() {
        val allPermissions = Permission.entries.toSet()
        val bytes =
            encodePairingCode(
                id = "all-perms",
                permissions = allPermissions,
            )

        val result = PairingCodeDecoder.decode(bytes)

        assertEquals(Permission.entries.size, result.grantedPermissions.size)
    }

    @Test
    fun `decode throws for unsupported version`() {
        val bytes = encodePairingCode(version = 99)

        assertFailsWith<IllegalArgumentException> {
            PairingCodeDecoder.decode(bytes)
        }
    }

    @Test
    fun `decode throws for invalid number of permissions`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, PairingCode.VERSION)
        BinaryEncodingUtils.writeString(writer, "test")
        BinaryEncodingUtils.writeLong(writer, 1700000000000L)
        BinaryEncodingUtils.writeInt(writer, 100) // Invalid: more than Permission.entries.size

        assertFailsWith<IllegalArgumentException> {
            PairingCodeDecoder.decode(writer.toByteArray())
        }
    }

    @Test
    fun `decode throws for negative number of permissions`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, PairingCode.VERSION)
        BinaryEncodingUtils.writeString(writer, "test")
        BinaryEncodingUtils.writeLong(writer, 1700000000000L)
        BinaryEncodingUtils.writeInt(writer, -1) // Invalid: negative

        assertFailsWith<IllegalArgumentException> {
            PairingCodeDecoder.decode(writer.toByteArray())
        }
    }

    @Test
    fun `decode handles unknown permission id gracefully`() {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, PairingCode.VERSION)
        BinaryEncodingUtils.writeString(writer, "test")
        BinaryEncodingUtils.writeLong(writer, 1700000000000L)
        BinaryEncodingUtils.writeInt(writer, 2) // 2 permissions
        BinaryEncodingUtils.writeInt(writer, Permission.OFFERBOOK.id) // Valid
        BinaryEncodingUtils.writeInt(writer, 999) // Invalid permission ID

        // Should not throw, just skip the invalid permission
        val result = PairingCodeDecoder.decode(writer.toByteArray())

        assertEquals(1, result.grantedPermissions.size)
        assertTrue(result.grantedPermissions.contains(Permission.OFFERBOOK))
    }
}
