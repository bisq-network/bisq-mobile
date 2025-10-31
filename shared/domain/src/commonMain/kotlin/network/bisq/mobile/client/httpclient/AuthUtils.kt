package network.bisq.mobile.client.httpclient

import io.ktor.http.Url
import kotlinx.io.bytestring.encodeToByteString
import network.bisq.mobile.domain.crypto.hmacSha256
import kotlin.random.Random.Default.nextBytes

fun generateAuthHash(
    password: String,
    nonce: String,
    timestamp: String,
    method: String,
    normalizedPath: String,
    bodySha256Hex: String?
): String {
    return hmacSha256(
        password.encodeToByteString().toByteArray(),
        "$nonce\n$timestamp\n${method.uppercase()}\n$normalizedPath\n${bodySha256Hex ?: ""}".encodeToByteString()
            .toByteArray(),
    ).toHexString()
}

fun getNormalizedPathAndQuery(url: Url): String {
    return url.encodedPath.let { if (it.length > 1) it.trimEnd('/') else it } + url.encodedQuery.let { if (it.isNotBlank()) "?$it" else "" }
}

fun generateNonce(): String {
    val nonceBytes = ByteArray(6)
    nextBytes(nonceBytes)
    return nonceBytes.toHexString()
}