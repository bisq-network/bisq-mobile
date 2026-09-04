package network.bisq.mobile.client.common.domain.service.chat.public_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.Citation

/**
 * Body of `POST /public-chat-channels/{channelId}/messages`.
 *
 * Its own type rather than trade chat's `SendChatMessageRequest`, which is `(text, citation)`: a
 * public channel has no identity of its own, so the sender travels in the body. Null means "the
 * node's selected identity", which is what
 * [network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade.sendChatMessage]
 * promises and what the node flavour does.
 */
@Serializable
data class SendPublicChatMessageRequest(
    val text: String,
    val citation: Citation?,
    val senderUserProfileId: String?,
)
