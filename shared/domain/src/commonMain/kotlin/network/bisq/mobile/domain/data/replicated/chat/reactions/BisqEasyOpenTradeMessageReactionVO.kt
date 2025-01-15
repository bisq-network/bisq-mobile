package network.bisq.mobile.domain.data.replicated.chat.reactions

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

@Serializable
data class BisqEasyOpenTradeMessageReactionVO(
    val id: String,
    val senderUserProfile: UserProfileVO,
    val receiverUserProfileId: String,
    val receiverNetworkId: NetworkIdVO,
    val chatChannelId: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val chatMessageId: String,
    val reactionId: Int,
    val date: Long,
    val isRemoved: Boolean
)