package network.bisq.mobile.client.httpclient

import io.ktor.http.Url
import kotlinx.io.bytestring.encodeToByteString
import network.bisq.mobile.domain.crypto.hmacSha256

fun generateAuthHash(
    password: String,
    timestamp: String,
    method: String,
    normalizedPath: String,
    bodySha256Hex: String?
): String {
    return hmacSha256(
        password.encodeToByteString().toByteArray(),
        "$timestamp\n${method.uppercase()}\n$normalizedPath\n${bodySha256Hex ?: ""}".encodeToByteString()
            .toByteArray(),
    ).toHexString()
}

fun getNormalizedPathAndQuery(url: Url): String {
    return url.encodedPath.let { if (it.length > 1) it.trimEnd('/') else it } + url.encodedQuery.let { if (it.isNotBlank()) "?$it" else "" }
}