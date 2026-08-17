package network.bisq.mobile.client.common.domain.service.chat.private_chat

import network.bisq.mobile.client.common.domain.service.chat.trade.SendChatMessageReactionRequest
import network.bisq.mobile.client.common.domain.service.chat.trade.SendChatMessageRequest
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.utils.Logging

/**
 * The trusted node's two-party private chat (DM) endpoints and subscriptions.
 *
 * Request bodies are shared with trade chat: the node uses the same records for both, so there is one
 * wire shape and no reason for a second copy here.
 */
class PrivateChatApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "private-chat-channels"

    // Rest API calls

    suspend fun findOrCreateChannel(peerProfileId: String): Result<FindOrCreateChannelResponse> {
        // Profile ids are hex, so no url path encoding is required according to RFC 3986
        val path = "$basePath/peers/$peerProfileId/channel"
        return webSocketApiClient.post(path, "")
    }

    suspend fun sendTextMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit> {
        // ChannelId does not contain characters which require url path encoding according to RFC 3986
        val path = "$basePath/$channelId/messages"
        return webSocketApiClient.post<Unit, SendChatMessageRequest>(path, SendChatMessageRequest(text, citation))
    }

    suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> {
        val path = "$basePath/$channelId/$messageId/reactions"
        val request = SendChatMessageReactionRequest(reactionEnum.ordinal, false, null)
        return webSocketApiClient.post<Unit, SendChatMessageReactionRequest>(path, request)
    }

    suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: TwoPartyPrivateChatMessageReaction,
    ): Result<Unit> {
        val path = "$basePath/$channelId/$messageId/reactions"
        val request =
            SendChatMessageReactionRequest(
                reaction.reactionId,
                true,
                reaction.senderUserProfile.id,
            )
        return webSocketApiClient.post<Unit, SendChatMessageReactionRequest>(path, request)
    }

    suspend fun leaveChannel(channelId: String): Result<Unit> = webSocketApiClient.post("$basePath/$channelId/leave", "")

    suspend fun consumeNotifications(channelId: String): Result<Unit> = webSocketApiClient.post("$basePath/$channelId/consume-notifications", "")

    // Subscriptions

    suspend fun subscribeChannels(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.PRIVATE_CHAT_CHANNELS)

    suspend fun subscribeMessages(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.PRIVATE_CHAT_MESSAGES)

    suspend fun subscribeReactions(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.PRIVATE_CHAT_REACTIONS)
}
