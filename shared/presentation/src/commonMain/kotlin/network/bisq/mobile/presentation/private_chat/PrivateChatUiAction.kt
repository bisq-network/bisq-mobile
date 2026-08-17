package network.bisq.mobile.presentation.private_chat

import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction

sealed interface PrivateChatUiAction {
    data class OnSendMessage(
        val text: String,
    ) : PrivateChatUiAction

    data class OnAddReaction(
        val message: TwoPartyPrivateChatMessage,
        val reaction: ReactionEnum,
    ) : PrivateChatUiAction

    data class OnRemoveReaction(
        val message: TwoPartyPrivateChatMessage,
        val reaction: TwoPartyPrivateChatMessageReaction,
    ) : PrivateChatUiAction

    /** Null clears the quote. */
    data class OnReply(
        val message: TwoPartyPrivateChatMessage?,
    ) : PrivateChatUiAction

    /** Opens the peer profile from the header. */
    data object OnPeerHeaderClick : PrivateChatUiAction

    /** Opens the peer profile from a message avatar or username. */
    data class OnPeerProfileClick(
        val profileId: String,
    ) : PrivateChatUiAction

    data class OnIgnoreUserClick(
        val profileId: String,
    ) : PrivateChatUiAction

    data object OnConfirmIgnore : PrivateChatUiAction

    data object OnDismissIgnoreDialog : PrivateChatUiAction

    data class OnUndoIgnoreUserClick(
        val profileId: String,
    ) : PrivateChatUiAction

    data object OnConfirmUndoIgnore : PrivateChatUiAction

    data object OnDismissUndoIgnoreDialog : PrivateChatUiAction

    data class OnReportUserClick(
        val message: TwoPartyPrivateChatMessage,
    ) : PrivateChatUiAction

    data object OnDismissReportDialog : PrivateChatUiAction

    /**
     * @param reportMessage what the user had typed, kept so the dialog can be reopened with it.
     *   `ReportUserPresenter` has already surfaced the error itself.
     */
    data class OnReportFailure(
        val reportMessage: String,
    ) : PrivateChatUiAction

    data object OnLeaveChatClick : PrivateChatUiAction

    data object OnConfirmLeave : PrivateChatUiAction

    data object OnDismissLeaveDialog : PrivateChatUiAction

    data object OnOpenChatRules : PrivateChatUiAction

    data object OnDontShowAgainChatRulesWarningBox : PrivateChatUiAction

    /** Raised by the message list as the user scrolls; marks the conversation read. */
    data class OnUpdateReadCount(
        val count: Int,
    ) : PrivateChatUiAction

    data object OnChannelNotFoundDialogDismiss : PrivateChatUiAction
}
