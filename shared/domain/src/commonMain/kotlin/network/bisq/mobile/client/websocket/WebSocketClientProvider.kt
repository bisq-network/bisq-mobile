package network.bisq.mobile.client.websocket

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

/**
 * Provider to handle dynamic host/port changes
 */
class WebSocketClientProvider(
    private val defaultHost: String,
    private val defaultPort: Int,
    private val settingsRepository: SettingsRepository,
    private val clientFactory: (String, Int) -> WebSocketClient) : Logging {

    companion object {
        fun parseUri(uri: String): Pair<String,Int> {
            uri.split("//")[1].split(":").let {
                return Pair(it[0], it[1].toInt())
            }
        }
    }

    private val ioScope = CoroutineScope(IODispatcher)

    @Volatile
    private var currentClient: WebSocketClient? = null
    private var connectionReady = CompletableDeferred<Boolean>()

    suspend fun testClient(host: String, port: Int): Boolean {
        val client = createClient(host, port)
        // not including path websocket will get connection refused
        val url = "ws://$host:$port/websocket"
        return try {
            if (client.isDemo()) {
                ApplicationBootstrapFacade.isDemo = true
                return true
            }
            // if connection is refused, catch will execute returning false
            client.connect(true)
            return true
        } catch (e: Exception) {
            log.e("Error testing connection to $url: ${e.message}")
            false
        } finally {
            client.disconnect(true) // Ensure the client is closed to free resources
        }
    }

    private fun createClient(host: String, port: Int): WebSocketClient {
        return clientFactory(host, port)
    }

    fun get(): WebSocketClient {
        if (currentClient == null) {
            runBlocking {
                lunchObserveSettingsChange()
                settingsRepository.fetch()
                connectionReady.await()
            }
        }
        return currentClient!!
    }

    private fun lunchObserveSettingsChange() {
        // Listen to changes in WebSocket configuration and update the client
        ioScope.launch {
            try {
                val mutex = Mutex()
                settingsRepository.data.collect { newSettings ->
                    mutex.withLock {
                        var host = defaultHost
                        var port = defaultPort
                        newSettings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            log.d { "new bisq url detected $url "}
                            parseUri(url).apply {
                                host = first
                                port = second
                            }
                        }
                        // only update if there was actually a change
                        if (currentClient == null || currentClient!!.host != host || currentClient!!.port != port) {
                            if (currentClient != null) {
                                log.d { "trusted node changing from ${currentClient!!.host}:${currentClient!!.port} to $host:$port" }
                            }
                            if (currentClient?.isConnected() == true) {
                                currentClient?.disconnect()
                            }
                            currentClient = createClient(host, port)
                            log.d { "Websocket client updated with url $host:$port" }
                            log.d { "Websocket client - connecting" }
                            currentClient?.connect()
                            if (!connectionReady.isCompleted) {
                                connectionReady.complete(true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Error updating WebSocket client with new settings." }
            }
        }
    }
}