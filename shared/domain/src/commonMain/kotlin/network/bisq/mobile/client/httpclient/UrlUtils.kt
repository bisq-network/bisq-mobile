package network.bisq.mobile.client.httpclient

internal fun sanitizeBaseUrl(raw: String, defaultPort: Int): String {
    val trimmed = raw.trim()
    var result = trimmed
    val lower = trimmed.lowercase()
    if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
        // Strip any leading scheme like "localhost://" or custom schemes
        val withoutScheme = trimmed.replaceFirst(Regex("^[A-Za-z][A-Za-z0-9+.-]*://"), "")
        result = "http://$withoutScheme"
    }
    // Ensure port present; if missing, append default port
    val hasPort = Regex("^https?://[^/]+:\\d+").containsMatchIn(result)
    if (!hasPort) {
        // Insert :port after host (before any path if present)
        result = result.replace(Regex("^(https?://)([^/]+)")) { m ->
            val scheme = m.groupValues[1]
            val host = m.groupValues[2]
            "${scheme}${host}:$defaultPort"
        }
    }
    return result
}

