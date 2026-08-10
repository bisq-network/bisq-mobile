package network.bisq.mobile.client.common.domain.access.pairing

import network.bisq.mobile.client.common.domain.utils.BinaryDecodingUtils
import network.bisq.mobile.domain.utils.getLogger
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

@OptIn(ExperimentalEncodingApi::class)
object PairingCodeDecoder {
    // Sanity cap against corrupt input only — deliberately NOT bound to Permission.entries.size:
    // a newer node may grant permissions this app version does not know yet, and the code must
    // still decode (unknown ids are skipped below). Bounding by the local enum size would make
    // pairing fail entirely against upgraded nodes.
    internal const val MAX_ENCODED_PERMISSIONS = 64

    fun decode(base64: String): PairingCode {
        val bytes =
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .decode(base64)
        return decode(bytes)
    }

    fun decode(bytes: ByteArray): PairingCode {
        val reader = BinaryDecodingUtils(bytes)

        val version = reader.readByte()
        if (version != PairingCode.Companion.VERSION) {
            throw UnsupportedPairingVersionException("Unsupported pairing code version: $version")
        }

        val id = reader.readString()
        val expiresAt =
            Instant.fromEpochMilliseconds(reader.readLong())

        val numPermissions = reader.readInt()
        require(numPermissions in 0..MAX_ENCODED_PERMISSIONS) {
            "Invalid number of permissions: $numPermissions"
        }

        val permissions = mutableSetOf<Permission>()
        repeat(numPermissions) {
            // Read outside the try: a truncated payload must fail as invalid format, while an
            // unknown id (newer node) is skipped as an unknown permission.
            val permissionId = reader.readInt()
            try {
                permissions += Permission.Companion.fromId(permissionId)
            } catch (e: Exception) {
                getLogger("").w { "Permission could not be resolved. {${e.message}}" }
            }
        }

        return PairingCode(
            id = id,
            expiresAt = expiresAt,
            grantedPermissions = permissions,
        )
    }
}
