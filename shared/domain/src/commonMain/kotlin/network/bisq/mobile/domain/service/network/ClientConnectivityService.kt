package network.bisq.mobile.domain.service.network

import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.domain.utils.Logging

@Suppress("RedundantOverride")
class ClientConnectivityService(
    private val webSocketClientService: WebSocketClientService
) : ConnectivityService(), Logging {
    override fun isConnected(): Boolean {
        return webSocketClientService.isConnected()
    }
}