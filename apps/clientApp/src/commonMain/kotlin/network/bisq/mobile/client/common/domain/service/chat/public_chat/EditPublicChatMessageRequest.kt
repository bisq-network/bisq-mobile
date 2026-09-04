package network.bisq.mobile.client.common.domain.service.chat.public_chat

import kotlinx.serialization.Serializable

/**
 * Body of `PUT /public-chat-channels/{channelId}/messages/{messageId}`.
 *
 * No sender: bisq2 authorises the edit against the message's own author, so naming one would only be
 * a second chance to name the wrong one.
 */
@Serializable
data class EditPublicChatMessageRequest(
    val text: String,
)
