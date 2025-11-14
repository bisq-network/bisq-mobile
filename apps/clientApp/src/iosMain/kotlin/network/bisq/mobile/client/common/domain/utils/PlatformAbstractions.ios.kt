package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig

actual fun createHttpClient(
    proxyConfig: BisqProxyConfig?,
    config: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(Darwin) {
    config(this)
    install(WebSockets) {
        pingIntervalMillis = 15_000 // not supported by okhttp engine
    }
    engine {
        proxy = proxyConfig?.config
    }
}