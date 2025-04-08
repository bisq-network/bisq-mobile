package network.bisq.mobile.client.service.chat.trade

import kotlinx.serialization.Serializable

@Serializable
data class SendChatMessageReactionRequest(val reactionId: Int, val isRemoved: Boolean, val senderUserProfileId: String?)
