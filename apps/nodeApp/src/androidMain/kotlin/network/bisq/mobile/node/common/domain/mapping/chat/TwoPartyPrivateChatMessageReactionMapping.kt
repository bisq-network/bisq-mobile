package network.bisq.mobile.node.common.domain.mapping.chat

import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.node.common.domain.mapping.Mappings
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction as Bisq2TwoPartyPrivateChatMessageReaction

/**
 * Inbound only, per the replicated data conventions: nothing needs to turn a mobile reaction back
 * into a Bisq 2 one — removing a reaction re-derives it from the reaction id instead.
 */
fun Bisq2TwoPartyPrivateChatMessageReaction.toDomain(): TwoPartyPrivateChatMessageReaction =
    TwoPartyPrivateChatMessageReaction(
        id,
        Mappings.UserProfileMapping.fromBisq2Model(senderUserProfile),
        receiverUserProfileId,
        Mappings.NetworkIdMapping.fromBisq2Model(receiverNetworkId),
        chatChannelId,
        Mappings.ChatChannelDomainMapping.fromBisq2Model(chatChannelDomain),
        chatMessageId,
        reactionId,
        date,
        isRemoved,
    )
