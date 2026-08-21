package network.bisq.mobile.client.common.domain.service.chat.private_chat

import kotlinx.serialization.Serializable

/** Response of `POST /private-chat-channels/peers/{peerProfileId}/channel`. */
@Serializable
data class FindOrCreateChannelResponse(
    val channelId: String,
)
