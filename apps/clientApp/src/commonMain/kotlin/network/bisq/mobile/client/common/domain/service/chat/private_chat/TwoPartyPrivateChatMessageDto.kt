package network.bisq.mobile.client.common.domain.service.chat.private_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Transport shape of bisq2's `TwoPartyPrivateChatMessageDto`. This is
 * [network.bisq.mobile.client.common.domain.service.chat.trade.BisqEasyOpenTradeMessageDto] without
 * the trade fields, mirroring upstream where a two-party message adds nothing to a private one.
 */
@Serializable
data class TwoPartyPrivateChatMessageDto(
    val messageId: String,
    val channelId: String,
    val senderUserProfile: UserProfileVO,
    val receiverUserProfileId: String,
    val receiverNetworkId: NetworkIdVO,
    val text: String?,
    val citation: Citation?,
    val date: Long,
    val chatMessageType: ChatMessageTypeEnum,
    val chatMessageReactions: Set<TwoPartyPrivateChatMessageReactionDto>,
    val citationAuthorUserProfile: UserProfileVO?,
)
