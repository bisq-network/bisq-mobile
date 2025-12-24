package network.bisq.mobile.crypto

import java.security.MessageDigest

actual fun getSha256(data: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(data)
}
