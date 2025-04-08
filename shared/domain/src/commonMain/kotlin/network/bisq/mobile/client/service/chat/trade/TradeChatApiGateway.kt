package network.bisq.mobile.client.service.chat.trade

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class TradeChatApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {

    // Subscriptions
    suspend fun subscribeTradeChats(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.TRADE_CHATS)
    }

    suspend fun subscribeChatReactions(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.CHAT_REACTIONS)
    }
}

