package network.bisq.mobile.presentation.community.public_chat

import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * State of a public chat thread — the hub's Discussions segment and the pushed Support screen.
 *
 * [messages] holds domain models rather than flattened rows, for the reason `PrivateChatUiState`
 * documents: `ChatMessageList` subscribes to a `StateFlow` on each message for its reactions, so
 * flattening them here would lose those updates.
 *
 * The structural difference from a DM is that ignore, report and profile targets are per message —
 * a public channel has no fixed peer — which is why they are ids and profiles here rather than flags.
 */
data class PublicChatUiState(
    val channelId: String = "",
    /** Newest first, then filtered by [searchQuery] and by the ignore list. */
    val messages: List<CommonPublicChatMessage> = emptyList(),
    val ignoredProfileIds: Set<String> = emptySet(),
    /**
     * Messages already read, which is what `ChatMessageList` expects — it derives the unread count as
     * `messages.size - readCount`. Seeded from the channel's unread count as it was *before* opening
     * consumed it, then owned by what the list reports back. -1 means "not resolved yet" and
     * suppresses the list, matching the private and trade threads.
     */
    val readCount: Int = -1,
    val searchQuery: String = "",
    val searchMatchCount: Int = 0,
    val quotedMessage: CommonPublicChatMessage? = null,
    val editingMessageId: String? = null,
    val editingInitialText: String = "",
    val deleteTargetMessageId: String? = null,
    val ignoreTargetProfileId: String? = null,
    val undoIgnoreTargetProfileId: String? = null,
    val reportTargetUserProfile: UserProfileVO? = null,
    /** Survives a failed report so reopening the dialog restores what the user typed. */
    val reportDraft: String? = null,
    val showChatRulesWarnBox: Boolean = false,
    val isLoading: Boolean = true,
    val isSupported: Boolean = true,
    /**
     * Raw-channel authors plus owned profiles. Kept off [messages] so a search or ignore
     * cannot shrink the picker — desktop offers ignored authors too.
     */
    val mentionCandidates: List<UserProfileVO> = emptyList(),
    /** Owned profiles the inbound highlighter matches against. */
    val myProfiles: List<UserProfileVO> = emptyList(),
) {
    /**
     * Derived rather than stored: a separate flag would be a second source of truth for the same
     * thing, and the search field is always on screen so there is no toggle to track.
     */
    val isSearching: Boolean get() = searchQuery.isNotBlank()
}
