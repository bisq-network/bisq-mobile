package network.bisq.mobile.data.service.chat.trade

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.service.LifeCycleAware

interface TradeChatMessagesServiceFacade : LifeCycleAware {
    /**
     * True once the node has delivered the trade chat messages, so a channel with no messages is an
     * empty channel rather than one whose messages are still on their way. Until then an empty channel
     * says nothing, which is what the trade chat screen waits on before it stops showing its spinner.
     */
    val chatMessagesSynced: StateFlow<Boolean>

    /**
     * True while the trade chat messages cannot be delivered at all: on the client, a subscribe that
     * failed, which is only retried on the next reconnect. [chatMessagesSynced] will not turn true until
     * this clears, so the trade chat screen stops waiting and renders what it has. Never true on the
     * node, whose messages come from its own store.
     */
    val chatMessagesSyncFailed: StateFlow<Boolean>

    suspend fun sendChatMessage(
        text: String,
        citation: Citation?,
    ): Result<Unit>

    suspend fun addChatMessageReaction(
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit>

    suspend fun removeChatMessageReaction(
        messageId: String,
        reaction: BisqEasyOpenTradeMessageReaction,
    ): Result<Boolean>
}
