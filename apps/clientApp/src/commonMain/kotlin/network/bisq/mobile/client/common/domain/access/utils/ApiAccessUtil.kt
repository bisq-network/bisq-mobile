package network.bisq.mobile.client.common.domain.access.utils

import io.ktor.http.Url
import io.ktor.http.parseUrl
import network.bisq.mobile.client.common.domain.access.LOCALHOST
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIpv4

object ApiAccessUtil {
    fun parseAndNormalizeUrl(value: String): Url? {
        val raw = value.trim()
        val withScheme = if (raw.contains("://")) raw else "http://$raw"
        val first =
            parseUrl(withScheme)
                ?: return null
        val hasExplicitPort =
            Regex("^https?://[^/]+:\\d+").containsMatchIn(withScheme)
        val host = first.host
        val needsDefaultPort =
            !hasExplicitPort && (
                host == LOCALHOST || host.isValidIpv4() ||
                    host.endsWith(
                        ".onion",
                        ignoreCase = true,
                    )
            )
        val port = if (needsDefaultPort) 8090 else first.port
        val normalized = "${first.protocol.name}://$host:$port"
        return parseUrl(normalized)
    }

    fun getProxyOptionFromRestUrl(url: String): BisqProxyOption {
        var proxyOption = BisqProxyOption.NONE
        val parsedUrl = parseAndNormalizeUrl(url)
        if (parsedUrl != null) {
            if (parsedUrl.host.endsWith(".onion", ignoreCase = true)) {
                proxyOption = BisqProxyOption.INTERNAL_TOR
            }
        }
        return proxyOption
    }
}
