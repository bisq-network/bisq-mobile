package network.bisq.mobile.client.common.domain.access.pairing

import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.utils.BinaryDecodingUtils
import network.bisq.mobile.domain.utils.getLogger
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PairingCodeDecoder {
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
        require(version == PairingCode.Companion.VERSION) {
            "Unsupported version"
        }

        val id = reader.readString()
        val expiresAt =
            Instant.Companion.fromEpochMilliseconds(reader.readLong())

        val numPermissions = reader.readInt()
        require(numPermissions in 0..Permission.entries.size) {
            "Invalid number of permissions: $numPermissions"
        }

        val permissions = mutableSetOf<Permission>()
        repeat(numPermissions) {
            try {
                permissions += Permission.Companion.fromId(reader.readInt())
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
