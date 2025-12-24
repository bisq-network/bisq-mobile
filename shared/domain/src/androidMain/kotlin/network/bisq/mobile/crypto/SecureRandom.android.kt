package network.bisq.mobile.crypto

import java.security.SecureRandom

actual fun nextSecureRandomBytes(count: Int): ByteArray {
    require(count >= 0) { "count must be >= 0" }
    if (count == 0) {
        return ByteArray(0)
    }
    val bytes = ByteArray(count)
    SecureRandom().nextBytes(bytes)
    return bytes
}
