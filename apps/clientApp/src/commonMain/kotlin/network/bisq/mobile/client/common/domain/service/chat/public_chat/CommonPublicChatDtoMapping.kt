package network.bisq.mobile.client.common.domain.service.chat.public_chat

import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * The transport types are a client concern; the shared models below are what both apps present. The
 * node produces the very same models straight from its embedded Bisq 2 types.
 */

fun CommonPublicChatMessageReactionDto.toDomain(): CommonPublicChatMessageReaction =
    CommonPublicChatMessageReaction(
        id = id,
        userProfileId = senderUserProfileId,
        chatChannelId = chatChannelId,
        chatChannelDomain = chatChannelDomain,
        chatMessageId = chatMessageId,
        reactionId = reactionId,
        date = date,
    )

/**
 * @param myUserProfile the selected profile, which decides reaction ownership only.
 * @param isMyMessage stated by the caller rather than derived, because bisq2 authorises edit and
 *   delete against ANY of my identities while reactions belong to the selected one. The DTO carries
 *   no such flag — the node computes the same thing from its `UserIdentityService`.
 * @param chatReactions comes from the reactions topic rather than from the message's own
 *   [CommonPublicChatMessageDto.chatMessageReactions], which is a snapshot from the moment the
 *   message was pushed.
 */
fun CommonPublicChatMessageDto.toDomain(
    myUserProfile: UserProfileVO,
    isMyMessage: Boolean,
    chatReactions: List<CommonPublicChatMessageReaction>,
): CommonPublicChatMessage =
    CommonPublicChatMessage(
        id = messageId,
        chatMessageType = chatMessageType,
        text = text,
        citation = citation,
        citationAuthorUserProfile = citationAuthorUserProfile,
        date = date,
        senderUserProfile = authorUserProfile,
        myUserProfile = myUserProfile,
        chatReactions = chatReactions,
        wasEdited = wasEdited,
        isMyMessage = isMyMessage,
    )

/**
 * Does not carry [CommonPublicChatChannelDto.unreadCount] over; the caller sets it on the model, so
 * that a channel arriving again with a moved count updates the instance a screen is already reading.
 *
 * `channelTitle` is taken from the id rather than from [CommonPublicChatChannelDto.title], which is
 * bisq2's `getDisplayString()` — a string already resolved against the NODE's bundle and locale,
 * where mobile needs the raw title it builds `"<domain>.<channelTitle>.title"` from. The id is
 * `"<domain>.<channelTitle>"`, so the tail is that raw title, and a migrated id only ever ends in
 * `bisq` or `support`. The node twin derives it the same way, and for the same reason.
 */
fun CommonPublicChatChannelDto.toDomain(): CommonPublicChatChannel =
    CommonPublicChatChannel(
        id = id,
        chatChannelDomain = chatChannelDomain,
        channelTitle = id.substringAfterLast('.'),
    )
