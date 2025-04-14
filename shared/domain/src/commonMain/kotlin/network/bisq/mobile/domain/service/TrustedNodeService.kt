package network.bisq.mobile.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.Logging

/**
 * This service allows to interact with the underlaying connectivity system
 * against the trusted node for the client.
 */
class TrustedNodeService(private val webSocketClientProvider: WebSocketClientProvider) : Logging {
    private val ioScope = CoroutineScope(IODispatcher)

    var isConnected: Boolean = false
    private var observingConnectivity = false

    /**
     * Connects to the trusted node, throws an exception if connection fails
     */
    suspend fun connect() {
        if (!observingConnectivity) {
            observeConnectivity()
        }
        runCatching {
            // first test connect and proceed to establish it if test passes
            webSocketClientProvider.get().let {
                if (!it.isDemo()) {
                    it.connect(true)
                    it.connect()
                }
            }
        }.onSuccess {
            log.d { "Connected to trusted node" }
        }.onFailure {
            log.e(it) { "ERROR: FAILED to connect to trusted node - details above" }
            throw it
        }
    }

    suspend fun disconnect() {
        // TODO
    }

    private fun observeConnectivity() {
        ioScope.launch {
            webSocketClientProvider.get().webSocketClientStatus.collect {
                log.d { "connectivity status changed - connected = $it" }
                isConnected = webSocketClientProvider.get().isConnected()
            }
        }
        observingConnectivity = true
    }

    fun isDemo() = webSocketClientProvider.get().isDemo()
}