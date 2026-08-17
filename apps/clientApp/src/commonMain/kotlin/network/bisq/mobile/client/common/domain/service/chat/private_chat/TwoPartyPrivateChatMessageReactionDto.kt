package network.bisq.mobile.client.common.domain.service.chat.private_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Transport shape of bisq2's `TwoPartyPrivateChatMessageReactionDto`.
 *
 * A DTO rather than the shared
 * [network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction] even though
 * the two are field-identical: shared models are not transport types.
 */
@Serializable
data class TwoPartyPrivateChatMessageReactionDto(
    val id: String,
    val senderUserProfile: UserProfileVO,
    val receiverUserProfileId: String,
    val receiverNetworkId: NetworkIdVO,
    val chatChannelId: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val chatMessageId: String,
    val reactionId: Int,
    val date: Long,
    val isRemoved: Boolean,
)
