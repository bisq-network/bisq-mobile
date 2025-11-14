package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig
import network.bisq.mobile.client.httpclient.NoDns
import java.util.concurrent.TimeUnit

actual fun createHttpClient(proxyConfig: BisqProxyConfig?, config: HttpClientConfig<*>.() -> Unit) =
    HttpClient(OkHttp) {
        config(this)
        install(WebSockets)
        engine {
            proxy = proxyConfig?.config
            config {
                if (proxyConfig?.isTorProxy == true) {
                    dns(NoDns())
                }
                pingInterval(15, TimeUnit.SECONDS)
            }
        }
    }