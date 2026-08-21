package network.bisq.mobile.client.common.domain.service.chat.private_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Transport shape of bisq2's `TwoPartyPrivateChatChannelDto`.
 *
 * Carries [myUserProfile] rather than a user identity: a DM is bound to whichever identity created or
 * received it, but the UI only ever needs the profile and key material has no business on the wire.
 * [unreadCount] is the node's persisted notification count, re-sent whenever it changes.
 */
@Serializable
data class TwoPartyPrivateChatChannelDto(
    val id: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val peer: UserProfileVO,
    val myUserProfile: UserProfileVO,
    val unreadCount: Long,
)
