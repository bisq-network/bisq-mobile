package network.bisq.mobile.domain.service

import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.utils.Logging

/**
 * This service allows to interact with the underlying connectivity system
 * against the trusted node for the client.
 */
class TrustedNodeService(private val webSocketClientProvider: WebSocketClientProvider) : Logging {

    /**
     * Connects to the trusted node, throws an exception if connection fails
     */
    suspend fun connect(): Throwable? {
        webSocketClientProvider.get().let {
            val status = it.webSocketClientStatus.value
            val error = (status as? ConnectionState.Disconnected)?.error
            if (error == null && status is ConnectionState.Connected) {
                log.d { "Connected to trusted node" }
            } else {
                log.e(error) { "ERROR: FAILED to connect to trusted node - status: $status - ${error?.message}" }
                return error
            }
        }
        return null
    }

    fun isConnected(): Boolean {
        return webSocketClientProvider.isConnected()
    }
}