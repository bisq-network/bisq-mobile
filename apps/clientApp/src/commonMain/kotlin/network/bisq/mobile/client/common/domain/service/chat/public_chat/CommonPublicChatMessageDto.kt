package network.bisq.mobile.client.common.domain.service.chat.public_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Transport shape of bisq2's `CommonPublicChatMessageDto`.
 *
 * A public message names its author by id AND carries the resolved profile, because bisq2 drops a
 * message whose author it cannot resolve rather than sending one without. Both are kept: the id is
 * what decides whether the message is mine.
 *
 * [wasEdited] has no counterpart in private chat, which cannot edit at all. An edit is a removal plus
 * a new message that keeps the ORIGINAL [date], so without the flag an edit rewrites history in place.
 */
@Serializable
data class CommonPublicChatMessageDto(
    val messageId: String,
    val channelId: String,
    val authorUserProfileId: String,
    val authorUserProfile: UserProfileVO,
    val text: String?,
    val citation: Citation?,
    val citationAuthorUserProfile: UserProfileVO?,
    val date: Long,
    val chatMessageType: ChatMessageTypeEnum,
    val wasEdited: Boolean,
    val chatMessageReactions: Set<CommonPublicChatMessageReactionDto>,
)
