package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlinx.serialization.json.Json

/**
 * a factory to determine the implementation for websocket client to be demo or real
 */
class WebSocketClientFactory(private val jsonConfig: Json) {

    fun createNewClient(httpClient: HttpClient, apiUrl: Url, password: String? = null): WebSocketClient {
        return if (apiUrl.host == "demo.bisq" && apiUrl.port == 21) {
            WebSocketClientDemo(jsonConfig)
        } else {
            WebSocketClientImpl(
                httpClient,
                jsonConfig,
                apiUrl,
                password,
            )
        }
    }
}