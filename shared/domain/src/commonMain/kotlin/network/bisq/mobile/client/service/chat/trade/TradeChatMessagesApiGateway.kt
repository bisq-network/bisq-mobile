package network.bisq.mobile.client.service.chat.trade

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.utils.Logging

class TradeChatMessagesApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {
    private val basePath = "trade-chat-channels"

    // Rest API calls
    suspend fun sendTextMessage(channelId: String, text: String, citationVO: CitationVO?): Result<Unit> {
        val request = SendChatMessageRequest(text, citationVO)
        // ChannelId does not contain characters which require url path encoding according to RFC 3986
        val path = "$basePath/$channelId/messages"
        return webSocketApiClient.post<Unit, SendChatMessageRequest>(path, request)
    }

    suspend fun addChatMessageReaction(channelId: String, messageId: String, reactionEnum: ReactionEnum): Result<Unit> {
        // ChannelId and messageId do not contain characters which require url path encoding according to RFC 3986
        val path = "$basePath/$channelId/$messageId/reactions"
        val request = SendChatMessageReactionRequest(reactionEnum.ordinal, false, null)
        return webSocketApiClient.post<Unit, SendChatMessageReactionRequest>(path, request)
    }

    suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        bisqEasyOpenTradeMessageReaction: BisqEasyOpenTradeMessageReactionVO
    ): Result<Unit> {
        // ChannelId and messageId do not contain characters which require url path encoding according to RFC 3986
        val path = "$basePath/$channelId/$messageId/reactions"
        val request = SendChatMessageReactionRequest(
            bisqEasyOpenTradeMessageReaction.reactionId,
            true,
            bisqEasyOpenTradeMessageReaction.senderUserProfile.id
        )
        return webSocketApiClient.post<Unit, SendChatMessageReactionRequest>(path, request)
    }

    // Subscriptions
    suspend fun subscribeTradeChats(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.TRADE_CHAT_MESSAGES)
    }

    suspend fun subscribeChatReactions(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.CHAT_REACTIONS)
    }
}

