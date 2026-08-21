package network.bisq.mobile.data.replicated.chat.two_party

import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.reactions.PrivateChatMessageReaction
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id

/**
 * A reaction on a private chat (DM) message, replicating Bisq 2's `TwoPartyPrivateChatMessageReaction`.
 *
 * Field-identical to
 * [network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction], because
 * upstream both types add nothing to `PrivateChatMessageReaction`. They stay separate anyway: they
 * are distinct network types in Bisq 2, and merging them would erase the type parameter that keeps
 * `PrivateChatServiceFacade.removeChatMessageReaction` from accepting a trade reaction.
 */
data class TwoPartyPrivateChatMessageReaction(
    override val id: String,
    override val senderUserProfile: UserProfileVO,
    override val receiverUserProfileId: String,
    override val receiverNetworkId: NetworkIdVO,
    override val chatChannelId: String,
    override val chatChannelDomain: ChatChannelDomainEnum,
    override val chatMessageId: String,
    override val reactionId: Int,
    override val date: Long,
    override val isRemoved: Boolean,
) : PrivateChatMessageReaction {
    override val userProfileId: String get() = senderUserProfile.id
}
