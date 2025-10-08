package network.bisq.mobile.client.websocket

import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.service.network.HttpClientProvider

/**
 * a factory to determine the implementation for websocket client to be demo or real
 */
class WebSocketClientFactory(private val jsonConfig: Json) {

    fun createNewClient(httpClientProvider: HttpClientProvider, host: String, port: Int): WebSocketClient {
        return if (host == "demo.bisq" && port == 21) {
            WebSocketClientDemo(jsonConfig)
        } else {
            WebSocketClientImpl(
                httpClientProvider.get(),
                jsonConfig,
                host,
                port,
            )
        }
    }
}