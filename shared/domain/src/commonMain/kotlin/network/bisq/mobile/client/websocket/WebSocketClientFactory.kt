package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * a factory to determine the implementation for websocket client to be demo or real
 */
class WebSocketClientFactory(private val jsonConfig: Json) {

    fun createNewClient(httpClient: HttpClient, host: String, port: Int): WebSocketClient {
        return if (host == "demo.bisq" && port == 21) {
            WebSocketClientDemo(jsonConfig)
        } else {
            WebSocketClientImpl(
                httpClient,
                jsonConfig,
                host,
                port,
            )
        }
    }
}