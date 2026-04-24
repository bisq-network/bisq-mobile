package network.bisq.mobile.data.utils

import io.ktor.http.DEFAULT_PORT
import io.ktor.http.parseUrl

/**
 * Produces a **log-safe** URL string: only scheme, host, and path (via [parseUrl]). Query and fragment
 * are not included (and userinfo is not re-serialized). The result is truncated to [maxLength].
 */
fun sanitizeUrlForLog(
    rawUrl: String,
    maxLength: Int = 256,
): String {
    val url = parseUrl(rawUrl.trim()) ?: return "invalid-url"
    return buildString {
        append(url.protocolOrNull?.name ?: "unknown")
        if (url.host.isNotEmpty()) {
            append("://")
            append(url.host)
            val port = url.specifiedPort
            val defaultPort = url.protocolOrNull?.defaultPort ?: url.protocol.defaultPort
            if (port != DEFAULT_PORT && port != defaultPort) {
                append(':')
                append(port)
            }
        }
        if (url.encodedPath.isNotEmpty()) {
            append(url.encodedPath)
        }
    }.take(maxLength)
}
