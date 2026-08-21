package network.bisq.mobile.client.common.domain.service.chat.private_chat

import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * The transport types are a client concern; the shared models below are what both apps present. The
 * node produces the very same models straight from its embedded Bisq 2 types.
 */

fun TwoPartyPrivateChatMessageReactionDto.toDomain(): TwoPartyPrivateChatMessageReaction =
    TwoPartyPrivateChatMessageReaction(
        id = id,
        senderUserProfile = senderUserProfile,
        receiverUserProfileId = receiverUserProfileId,
        receiverNetworkId = receiverNetworkId,
        chatChannelId = chatChannelId,
        chatChannelDomain = chatChannelDomain,
        chatMessageId = chatMessageId,
        reactionId = reactionId,
        date = date,
        isRemoved = isRemoved,
    )

/**
 * [chatReactions] comes from the reactions topic rather than from the message's own
 * `chatMessageReactions`: the message is only ever pushed once, when it arrives, so its embedded set
 * is a snapshot from that moment.
 */
fun TwoPartyPrivateChatMessageDto.toDomain(
    myUserProfile: UserProfileVO,
    chatReactions: List<TwoPartyPrivateChatMessageReaction>,
): TwoPartyPrivateChatMessage =
    TwoPartyPrivateChatMessage(
        id = messageId,
        chatMessageType = chatMessageType,
        text = text,
        citation = citation,
        citationAuthorUserProfile = citationAuthorUserProfile,
        date = date,
        senderUserProfile = senderUserProfile,
        myUserProfile = myUserProfile,
        chatReactions = chatReactions,
    )

/** Does not carry [TwoPartyPrivateChatChannelDto.unreadCount] over; the caller sets it on the model. */
fun TwoPartyPrivateChatChannelDto.toDomain(): TwoPartyPrivateChatChannel =
    TwoPartyPrivateChatChannel(
        id = id,
        chatChannelDomain = chatChannelDomain,
        peer = peer,
        myUserProfile = myUserProfile,
    )
