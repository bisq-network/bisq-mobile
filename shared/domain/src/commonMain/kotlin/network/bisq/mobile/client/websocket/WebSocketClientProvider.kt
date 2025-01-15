package network.bisq.mobile.client.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Provider to handle dynamic host/port changes
 */
class WebSocketClientProvider(
    defaultHost: String,
    defaultPort: Int,
    private val settingsRepository: SettingsRepository,
    private val clientFactory: (String, Int) -> WebSocketClient) : Logging {

    companion object {
        fun parseUri(uri: String): Pair<String,Int> {
            uri.split("//")[1].split(":").let {
                return Pair(it[0], it[1].toInt())
            }
        }
    }

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    @Volatile
    private var currentClient: WebSocketClient? = null

    init {
        // Listen to changes in WebSocket configuration and update the client
        // TODO we might need to replicate this for changes in settings to reconnect to channels
        backgroundScope.launch {
            try {
                settingsRepository.data.collect { newSettings ->
                    var host = defaultHost
                    var port = defaultPort
                    newSettings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        log.d { "new bisq url $url "}
                        parseUri(url).apply {
                            host = first
                            port = second
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

    @OptIn(ExperimentalUuidApi::class)
    suspend fun testClient(host: String, port: Int): Boolean {
        val client = createClient(host, port)
        val url = "ws://$host:$port"
        return try {
            // if connection is refused, catch will execute returning false
            client.connect()
            return client.isConnected
        } catch (e: Exception) {
            log.e("Error testing connection to $url: ${e.message}")
            false
        } finally {
            client.disconnect() // Ensure the client is closed to free resources
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