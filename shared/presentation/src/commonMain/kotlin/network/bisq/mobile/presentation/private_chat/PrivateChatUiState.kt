package network.bisq.mobile.presentation.private_chat

import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * State of the private chat (DM) screen (issue #590).
 *
 * [messages] holds domain models rather than immutable data, which deviates from the usual UiState
 * rule. `ChatMessageList` subscribes to `StateFlow`s on each message for reactions and delivery
 * status, so flattening them here would lose those updates. Trade chat already works this way, and
 * the list identity still changes on every emission, so recomposition stays correct.
 */
data class PrivateChatUiState(
    val channelId: String = "",
    val peerUserProfile: UserProfileVO? = null,
    val peerStarRating: Double = 0.0,
    /**
     * True while the peer's score could not be resolved, which is not the same as a score of zero —
     * the header then shows no stars at all. Mirrors `PeerProfileUiState.isReputationUnknown`.
     */
    val isPeerReputationUnknown: Boolean = false,
    val messages: List<TwoPartyPrivateChatMessage> = emptyList(),
    val quotedMessage: TwoPartyPrivateChatMessage? = null,
    val ignoredProfileIds: Set<String> = emptySet(),
    /**
     * Messages already read, which is what `ChatMessageList` expects — it derives the unread count
     * as `messages.size - readCount`. Seeded from the channel's unread count as it was *before*
     * opening consumed it, then owned by whatever the list reports back as the user scrolls.
     * -1 means "not resolved yet" and suppresses the list, matching `TradeChatScreen`.
     */
    val readCount: Int = -1,
    val showChatRulesWarnBox: Boolean = false,
    val isLoading: Boolean = true,
    val isChannelNotFound: Boolean = false,
    val showLeaveConfirmDialog: Boolean = false,
    /** The subject of both dialogs is always the peer, so a flag is all the screen needs. */
    val showIgnoreDialog: Boolean = false,
    val showUndoIgnoreDialog: Boolean = false,
    val showReportDialog: Boolean = false,
    /** Survives a failed report so reopening the dialog restores what the user typed. */
    val reportDraft: String? = null,
    /**
     * Raw-channel authors plus the conversation's two profiles (the peer and mine) — other
     * owned profiles are not mentionable in a DM. Kept off [messages] so an ignore cannot
     * hide the peer from the picker — desktop offers ignored authors too.
     */
    val mentionCandidates: List<UserProfileVO> = emptyList(),
    /** Owned profiles the inbound highlighter matches against. */
    val myProfiles: List<UserProfileVO> = emptyList(),
)
