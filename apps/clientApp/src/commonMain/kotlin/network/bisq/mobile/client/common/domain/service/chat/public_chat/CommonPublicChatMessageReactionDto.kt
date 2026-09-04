package network.bisq.mobile.client.common.domain.service.chat.public_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Transport shape of bisq2's `CommonPublicChatMessageReactionDto`.
 *
 * No `isRemoved`, unlike the private sibling: a public reaction is taken back by removing it from the
 * P2P store, so the topic pushes a real `REMOVED` carrying the original reaction and the client
 * deletes by [id].
 *
 * [senderUserProfile] is the resolved profile bisq2 sends alongside the id. The shared model keeps
 * only the id, so the mapping drops it; it is declared because it is what makes a removal impossible
 * to push once the sender's profile is pruned, which is one of the gaps a snapshot repairs.
 */
@Serializable
data class CommonPublicChatMessageReactionDto(
    val id: String,
    val senderUserProfileId: String,
    val senderUserProfile: UserProfileVO,
    val chatChannelId: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val chatMessageId: String,
    val reactionId: Int,
    val date: Long,
)
