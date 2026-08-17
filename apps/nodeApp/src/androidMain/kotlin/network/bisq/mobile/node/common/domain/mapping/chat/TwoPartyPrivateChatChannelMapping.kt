package network.bisq.mobile.node.common.domain.mapping.chat

import bisq.user.profile.UserProfile
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.node.common.domain.mapping.Mappings
import bisq.chat.two_party.TwoPartyPrivateChatChannel as Bisq2TwoPartyPrivateChatChannel

/**
 * @param peer resolved by the caller through `UserProfileService.getManagedUserProfile` rather than
 *   taken from `channel.peer`, which is the copy persisted with the channel and therefore predates any
 *   later republish of the peer's editable fields.
 */
fun Bisq2TwoPartyPrivateChatChannel.toDomain(peer: UserProfile): TwoPartyPrivateChatChannel =
    TwoPartyPrivateChatChannel(
        // Read through the accessors, never the raw fields: Bisq 2 migrates the deprecated
        // BISQ_EASY_PRIVATE_CHAT / EVENTS domains to DISCUSSION on read, and the channel id
        // is derived from the migrated domain.
        id = id,
        chatChannelDomain = Mappings.ChatChannelDomainMapping.fromBisq2Model(chatChannelDomain),
        peer = Mappings.UserProfileMapping.fromBisq2Model(peer),
        myUserProfile = Mappings.UserProfileMapping.fromBisq2Model(myUserIdentity.userProfile),
    )
