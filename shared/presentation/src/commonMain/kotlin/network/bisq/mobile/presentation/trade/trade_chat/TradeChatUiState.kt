package network.bisq.mobile.presentation.trade.trade_chat

import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * State of the trade chat screen.
 *
 * [messages] holds domain models rather than flattened rows, for the reason `PrivateChatUiState`
 * documents: `ChatMessageList` subscribes to a `StateFlow` on each message for reactions and
 * delivery status, so flattening them here would lose those updates.
 *
 * Ignore, report and profile targets are per message — a trade chat can include the peer, a
 * mediator, and protocol senders — which is why they are ids and messages here rather than flags.
 */
data class TradeChatUiState(
    val selectedTrade: TradeItemPresentationModel? = null,
    val messages: List<BisqEasyOpenTradeMessage> = emptyList(),
    val ignoredProfileIds: Set<String> = emptySet(),
    /**
     * Messages already read, which is what `ChatMessageList` expects — it derives the unread count
     * as `messages.size - readCount`. Seeded from `TradeReadStateRepository` for this trade, then
     * owned by what the list reports back. -1 means "not resolved yet" and suppresses the list.
     */
    val readCount: Int = -1,
    val quotedMessage: BisqEasyOpenTradeMessage? = null,
    val ignoreTargetProfileId: String? = null,
    val undoIgnoreTargetProfileId: String? = null,
    val reportTargetMessage: BisqEasyOpenTradeMessage? = null,
    /** Survives a failed report so reopening the dialog restores what the user typed. */
    val reportDraft: String? = null,
    /** The accused profile [reportDraft] belongs to; a different target must not inherit it. */
    val reportDraftProfileId: String? = null,
    val showChatRulesWarnBox: Boolean = false,
    /**
     * True until there is something to render: the trade has to resolve, and its messages arrive over
     * a subscription that on a cold start can land well after the screen opened. An empty message list
     * on its own cannot be told apart from a chat that has not loaded, so the screen state comes from
     * this flag, and the flag from `TradeChatMessagesServiceFacade.chatMessagesSynced` or, when the
     * messages are not coming at all, `TradeChatMessagesServiceFacade.chatMessagesSyncFailed`.
     */
    val isLoading: Boolean = true,
    val isTradeNotFound: Boolean = false,
    /**
     * Raw-channel authors plus this trade's participants: the peer, the mediator, and the
     * identity the trade is run with. Kept off [messages] so an ignore cannot hide a sender
     * from the picker — desktop offers ignored authors too.
     */
    val mentionCandidates: List<UserProfileVO> = emptyList(),
    /** Owned profiles the inbound highlighter matches against. */
    val myProfiles: List<UserProfileVO> = emptyList(),
)
