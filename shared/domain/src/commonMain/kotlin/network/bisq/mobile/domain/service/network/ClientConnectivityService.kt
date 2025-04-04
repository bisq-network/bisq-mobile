package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.utils.Logging

class ClientConnectivityService(
    private val webSocketClientProvider: WebSocketClientProvider
): ConnectivityService(), Logging {
    private val backgroundScope = CoroutineScope(BackgroundDispatcher)
    
    override fun isConnected(): Boolean {
        var connected = false
        backgroundScope.launch {
            runCatching {
                connected = webSocketClientProvider.get().isConnected()
            }
        }
        return connected
    }
    
    override fun onStart() {
        backgroundScope.launch {
            runCatching {
                val client = webSocketClientProvider.get()
                // Additional initialization if needed
            }
        }
    }
}