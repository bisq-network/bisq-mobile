package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.Logging

open class ClientConnectivityService(
    private val webSocketClientProvider: WebSocketClientProvider
): ConnectivityService(), Logging {
    private val backgroundScope = CoroutineScope(IODispatcher)
    
    override suspend fun isConnected(): Boolean {
        return webSocketClientProvider.get().isConnected()
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