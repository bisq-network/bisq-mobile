package network.bisq.mobile.client.common.domain.httpclient

import io.ktor.http.Url
import network.bisq.mobile.crypto.hmacSha256
import network.bisq.mobile.crypto.nextSecureRandomBytes

object AuthUtils {
    fun generateAuthHash(
        password: String,
        nonce: String,
        timestamp: String,
        method: String,
        normalizedPath: String,
        bodySha256Hex: String?,
    ): String {
        val key = password.encodeToByteArray()
        val canonical = "$nonce\n$timestamp\n${method.uppercase()}\n$normalizedPath\n${bodySha256Hex ?: ""}"
        return hmacSha256(key, canonical.encodeToByteArray()).toHexString()
    }

    fun getNormalizedPathAndQuery(url: Url): String = url.encodedPath.let { if (it.length > 1) it.trimEnd('/') else it } + url.encodedQuery.let { if (it.isNotBlank()) "?$it" else "" }

    fun generateNonce(bytesCount: Int = 8): String = nextSecureRandomBytes(bytesCount).toHexString()
}
