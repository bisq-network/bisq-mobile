package network.bisq.mobile.client.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

/**
 * Provider to handle dynamic host/port changes
 */
class WebSocketClientProvider(
    defaultHost: String,
    defaultPort: Int,
    private val settingsRepository: SettingsRepository,
    private val clientFactory: (String, Int) -> WebSocketClient) : Logging {

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    @Volatile
    private var currentClient: WebSocketClient? = null

    init {
        // Listen to changes in WebSocket configuration and update the client
        backgroundScope.launch {
            try {
                settingsRepository.data.collect { newSettings ->
                    var host = defaultHost
                    var port = defaultPort
                    newSettings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        log.d { "new bisq url $url "}
                        url.split("//")[1].split(":").let {
                            host = it[0]
                            port = it[1].toInt()
                        }
                    }
                    // only update if there was actually a change
                    if (currentClient == null || currentClient!!.host != host || currentClient!!.port != port) {
                        if (currentClient?.isConnected == true) {
                            currentClient?.disconnect()
                        }
                        log.d { "Websocket client updated with url $host:$port" }
                        currentClient = createClient(host, port)
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Error updating WebSocket client with new settings." }
            }
        }
    }

    private fun createClient(host: String, port: Int): WebSocketClient {
        return clientFactory(host, port)
    }

    fun get(): WebSocketClient {
        if (currentClient == null) {
            runBlocking {
                settingsRepository.fetch()
            }
        }
        return currentClient!!
    }
}