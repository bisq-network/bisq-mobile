package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig

/**
 * Implementations of this function are expected to handle DNS leak by preventing system dns resolution in case proxy is a tor proxy.
 */
expect fun createHttpClient(
    host: String,
    tlsFingerprint: String?,
    proxyConfig: BisqProxyConfig? = null,
    config: HttpClientConfig<*>.() -> Unit = {},
): HttpClient
