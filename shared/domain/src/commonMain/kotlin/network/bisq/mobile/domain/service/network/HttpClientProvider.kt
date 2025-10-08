package network.bisq.mobile.domain.service.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.createHttpClient
import network.bisq.mobile.domain.data.model.TorConfig
import network.bisq.mobile.domain.utils.Logging

class HttpClientProvider(private val json: Json) : Logging {
    fun get(): HttpClient {
        if (TorConfig.useTor) {
            val proxyConfig = ProxyBuilder.socks("127.0.0.1", KmpTorClientService.SOCKS_PORT)
            return createHttpClient(proxyConfig) {
                install(WebSockets)
                install(ContentNegotiation) {
                    json(json)
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 180_000
                    connectTimeoutMillis = 60_000
                    socketTimeoutMillis = 60_000
                }
            }
        } else {
            return createHttpClient() {
                install(WebSockets)
                install(ContentNegotiation) {
                    json(json)
                }
            }
        }
    }
}