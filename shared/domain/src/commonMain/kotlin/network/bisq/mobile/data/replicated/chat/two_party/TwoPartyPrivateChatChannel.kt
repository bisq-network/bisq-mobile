package network.bisq.mobile.data.replicated.chat.two_party

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * A two-party private chat channel (a DM with one peer), replicating Bisq 2's
 * `TwoPartyPrivateChatChannel`.
 *
 * [peer] and [myUserProfile] are plain profiles rather than a user identity: the UI only ever needs
 * the profile, and it keeps key material out of the shared layer.
 *
 * Both are fixed for the life of the model, and nothing the UI shows about them can move: a Bisq 2
 * nickname is immutable (`UserIdentityService.editUserProfile` only takes terms and statement) and
 * the avatar is derived from the profile's proof of work. The producers still re-resolve the peer
 * through `UserProfileService.getManagedUserProfile` before building a channel, as desktop does —
 * that swaps the copy persisted inside the channel for the one in the network store, refreshing the
 * editable fields (`publishDate`, terms, statement) and falling back to the embedded copy when the
 * profile has been pruned.
 */
class TwoPartyPrivateChatChannel(
    val id: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val peer: UserProfileVO,
    val myUserProfile: UserProfileVO,
) {
    private val _chatMessages: MutableStateFlow<Set<TwoPartyPrivateChatMessage>> = MutableStateFlow(emptySet())
    val chatMessages: StateFlow<Set<TwoPartyPrivateChatMessage>> = _chatMessages.asStateFlow()

    /**
     * Number of unread messages, sourced from Bisq 2's persisted `ChatNotificationService` rather
     * than counted locally, so it survives an app restart.
     */
    private val _unreadCount: MutableStateFlow<Long> = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    fun addChatMessage(message: TwoPartyPrivateChatMessage) {
        // Last write wins: equals/hashCode is not enough, because a message keeps its id while its
        // reactions and delivery status change.
        _chatMessages.update { current ->
            current
                .filterNot { it.id == message.id }
                .toSet() + message
        }
    }

    fun setAllChatMessages(messages: Set<TwoPartyPrivateChatMessage>) {
        _chatMessages.value = messages.associateBy { it.id }.values.toSet()
    }

    fun setUnreadCount(value: Long) {
        _unreadCount.value = value
    }
}
