package network.bisq.mobile.client.common.domain.service.chat.public_chat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum

/**
 * Transport shape of bisq2's `CommonPublicChatChannelDto`.
 *
 * [title] and [description] are declared to mirror the record but deliberately not mapped: bisq2
 * fills them with `getDisplayString()` and `getDescription()`, which are already resolved against the
 * NODE's `Res` bundle and locale. Mobile renders the channel from its own bundle, so taking them
 * would put the trusted node's language on the user's screen. See `CommonPublicChatDtoMapping`.
 *
 * [unreadCount] is not the channel's own: bisq2 injects it from `ChatNotificationService` and re-sends
 * the channel whenever it moves.
 */
@Serializable
data class CommonPublicChatChannelDto(
    val id: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val title: String,
    val description: String,
    val unreadCount: Long,
)
