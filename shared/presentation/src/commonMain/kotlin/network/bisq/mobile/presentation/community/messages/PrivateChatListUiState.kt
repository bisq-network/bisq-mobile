package network.bisq.mobile.presentation.community.messages

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * The Messages tab of the Community hub (#1825): the user's private conversations, newest
 * activity first. `isLoading` is true only until the facade's first emission is in, which is
 * what distinguishes "not loaded yet" from "genuinely no conversations".
 */
data class PrivateChatListUiState(
    val conversations: List<PrivateChatListItemUiState> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * One conversation row. [lastMessageEpochMillis] stays on the item (not just its formatted
 * label) so the presenter's newest-first ordering is testable against the raw value.
 */
data class PrivateChatListItemUiState(
    val channelId: String,
    val peerProfile: UserProfileVO,
    val starRating: Double,
    val lastMessagePreview: String,
    val lastMessageTimeLabel: String,
    val lastMessageEpochMillis: Long,
    val unreadCount: Long,
)

sealed interface PrivateChatListUiAction {
    data class OnConversationClick(
        val channelId: String,
    ) : PrivateChatListUiAction

    data object OnBrowseOfferbookClick : PrivateChatListUiAction
}
