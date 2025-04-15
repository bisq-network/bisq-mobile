package network.bisq.mobile.client.web.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.utils.Logging

class WebClientConnectivityService(
    private val webSocketClientProvider: WebSocketClientProvider
) : ClientConnectivityService(webSocketClientProvider), Logging {

    private val backgroundScope = CoroutineScope(IODispatcher)

    // Don't override these methods as they're already defined in the parent classes
    // and some are marked as final
    // Instead, you can add web-specific functionality here
    // For example, you might want to add methods to handle web-specific connectivity issues
    fun checkWebConnectivity() {
        backgroundScope.launch {
            // Web-specific connectivity check logic
            runCatching {
                // Check if navigator.onLine is true
                val isOnline = js("navigator.onLine")
                if (!isOnline) {
                    log.d { "Web browser is offline" }
                    // Handle offline state
                }
            }
        }
    }
}