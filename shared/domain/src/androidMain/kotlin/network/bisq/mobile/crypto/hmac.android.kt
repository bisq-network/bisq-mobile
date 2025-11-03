package network.bisq.mobile.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val hmacKey = SecretKeySpec(key, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(hmacKey)
    val hmacBytes = mac.doFinal(data)
    return hmacBytes
}