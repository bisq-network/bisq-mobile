package network.bisq.mobile.presentation.trade.trade_chat

import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum

sealed interface TradeChatUiAction {
    data class OnSendMessage(
        val text: String,
    ) : TradeChatUiAction

    data class OnResendMessage(
        val messageId: String,
    ) : TradeChatUiAction

    data class OnAddReaction(
        val message: BisqEasyOpenTradeMessage,
        val reaction: ReactionEnum,
    ) : TradeChatUiAction

    data class OnRemoveReaction(
        val message: BisqEasyOpenTradeMessage,
        val reaction: BisqEasyOpenTradeMessageReaction,
    ) : TradeChatUiAction

    /** Null clears the quote. */
    data class OnReply(
        val message: BisqEasyOpenTradeMessage?,
    ) : TradeChatUiAction

    data class OnPeerProfileClick(
        val profileId: String,
    ) : TradeChatUiAction

    data class OnIgnoreUserClick(
        val profileId: String,
    ) : TradeChatUiAction

    data object OnConfirmIgnore : TradeChatUiAction

    data object OnDismissIgnoreDialog : TradeChatUiAction

    data class OnUndoIgnoreUserClick(
        val profileId: String,
    ) : TradeChatUiAction

    data object OnConfirmUndoIgnore : TradeChatUiAction

    data object OnDismissUndoIgnoreDialog : TradeChatUiAction

    data class OnReportUserClick(
        val message: BisqEasyOpenTradeMessage,
    ) : TradeChatUiAction

    data object OnDismissReportDialog : TradeChatUiAction

    /**
     * @param reportMessage what the user had typed, kept so the dialog can be reopened with it.
     *   `ReportUserPresenter` has already surfaced the error itself.
     */
    data class OnReportFailure(
        val reportMessage: String,
    ) : TradeChatUiAction

    data object OnOpenChatRules : TradeChatUiAction

    data object OnDontShowAgainChatRulesWarningBox : TradeChatUiAction

    /** Raised by the message list as the user scrolls; marks the conversation read. */
    data class OnUpdateReadCount(
        val count: Int,
    ) : TradeChatUiAction

    data object OnTradeNotFoundDialogDismiss : TradeChatUiAction
}
