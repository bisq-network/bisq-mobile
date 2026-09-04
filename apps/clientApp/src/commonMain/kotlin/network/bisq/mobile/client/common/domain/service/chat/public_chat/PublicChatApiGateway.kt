package network.bisq.mobile.client.common.domain.service.chat.public_chat

import network.bisq.mobile.client.common.domain.service.chat.trade.SendChatMessageReactionRequest
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.utils.Logging

/**
 * The trusted node's public chat endpoints and subscriptions: the Discussions and Support channels.
 *
 * Channel and message ids need no percent-encoding: a channel id is `"<domain>.<title>"` and a
 * message id is a hash, both unreserved under RFC 3986. Same reasoning as
 * [network.bisq.mobile.client.common.domain.service.chat.private_chat.PrivateChatApiGateway], and
 * the ids are not user input in either.
 *
 * [SendChatMessageReactionRequest] is shared with trade and private chat, whose record bisq2 reuses
 * verbatim here. The message bodies are not: a public send names its sender, and an edit exists at
 * all only on this surface.
 */
class PublicChatApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "public-chat-channels"

    // Rest API calls. No channel or message GET: the WebSocket subscription snapshot carries the
    // full list, and nothing needs it over REST until a reload path exists.

    suspend fun sendTextMessage(
        channelId: String,
        text: String,
        citation: Citation?,
        senderUserProfileId: String?,
    ): Result<Unit> {
        val path = "$basePath/$channelId/messages"
        val request = SendPublicChatMessageRequest(text, citation, senderUserProfileId)
        return webSocketApiClient.post<Unit, SendPublicChatMessageRequest>(path, request)
    }

    suspend fun editMessage(
        channelId: String,
        messageId: String,
        text: String,
    ): Result<Unit> {
        val path = "$basePath/$channelId/messages/$messageId"
        return webSocketApiClient.put<Unit, EditPublicChatMessageRequest>(path, EditPublicChatMessageRequest(text))
    }

    suspend fun deleteMessage(
        channelId: String,
        messageId: String,
    ): Result<Unit> = webSocketApiClient.delete("$basePath/$channelId/messages/$messageId")

    /**
     * Note the path: private chat's reactions hang off `"/{channelId}/{messageId}/reactions"`, public
     * chat's off `"/{channelId}/messages/{messageId}/reactions"`.
     */
    suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
        senderUserProfileId: String?,
    ): Result<Unit> {
        val path = "$basePath/$channelId/messages/$messageId/reactions"
        val request = SendChatMessageReactionRequest(reactionEnum.ordinal, false, senderUserProfileId)
        return webSocketApiClient.post<Unit, SendChatMessageReactionRequest>(path, request)
    }

    /**
     * The sender comes from the reaction rather than from the selected identity: a public message
     * carries reactions from everyone, and bisq2 requires the owner to be named on a removal.
     */
    suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: CommonPublicChatMessageReaction,
    ): Result<Unit> {
        val path = "$basePath/$channelId/messages/$messageId/reactions"
        val request = SendChatMessageReactionRequest(reaction.reactionId, true, reaction.userProfileId)
        return webSocketApiClient.post<Unit, SendChatMessageReactionRequest>(path, request)
    }

    suspend fun consumeNotifications(channelId: String): Result<Unit> = webSocketApiClient.post("$basePath/$channelId/consume-notifications", "")

    // Subscriptions

    suspend fun subscribeChannels(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.PUBLIC_CHAT_CHANNELS)

    suspend fun subscribeMessages(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.PUBLIC_CHAT_MESSAGES)

    suspend fun subscribeReactions(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.PUBLIC_CHAT_REACTIONS)
}
