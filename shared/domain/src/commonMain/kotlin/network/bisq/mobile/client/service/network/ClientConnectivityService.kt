package network.bisq.mobile.client.service.network

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.utils.Logging

@Suppress("RedundantOverride")
class ClientConnectivityService(
    private val webSocketClientProvider: WebSocketClientProvider
) : ConnectivityService(), Logging {
    override fun isConnected(): Boolean {
        return webSocketClientProvider.get().isConnected()
    }
}