package network.bisq.mobile.domain.service

import kotlinx.coroutines.CoroutineScope
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.utils.Logging

/**
 * This service allows to interact with the underlaying connectivity system
 * against the trusted node for the client.
 */
class TrustedNodeService(private val webSocketClientProvider: WebSocketClientProvider) : Logging {
    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    // TODO websocketClient.isConnected should be observable so that we emit
    // events when disconnected and UI can react
    fun isConnected() = webSocketClientProvider.get().isConnected

    /**
     * Connects to the trusted node, throws an exception if connection fails
     */
    suspend fun connect() {
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
}