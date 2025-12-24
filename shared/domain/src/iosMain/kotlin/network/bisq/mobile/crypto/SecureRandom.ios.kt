package network.bisq.mobile.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual fun nextSecureRandomBytes(count: Int): ByteArray {
    require(count >= 0) { "count must be >= 0" }
    if (count == 0) {
        return ByteArray(0)
    }
    val out = ByteArray(count)
    val status =
        out.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, count.toULong(), pinned.addressOf(0))
        }
    if (status != errSecSuccess) {
        throw IllegalStateException("SecRandomCopyBytes failed: status=$status")
    }
    return out
}
