package network.bisq.mobile.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.utils.Logging

/**
 * This service allows to interact with the underlaying connectivity system
 * against the trusted node for the client.
 */
class TrustedNodeService(private val webSocketClientProvider: WebSocketClientProvider) : Logging {
    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    var isConnected: Boolean = false
    var observingConnectivity = false

    /**
     * Connects to the trusted node, throws an exception if connection fails
     */
    suspend fun connect() {
        if (!observingConnectivity) {
            observeConnectivity()
        }
        runCatching {
            webSocketClientProvider.get().connect()
        }.onSuccess {
            log.d { "Connected to trusted node" }
        }.onFailure {
            log.e { "ERROR: FAILED to connect to trusted node - details above" }
            throw it
        }
    }

    suspend fun disconnect() {
        // TODO
    }

    private fun observeConnectivity() {
        backgroundScope.launch {
            webSocketClientProvider.get().connected.collect {
                log.d { "connectivity status changed - connected = $it" }
                isConnected = webSocketClientProvider.get().isConnected()
            }
        }
        observingConnectivity = true
    }
}