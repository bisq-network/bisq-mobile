package network.bisq.mobile.data.service.chat.trade

import network.bisq.mobile.data.replicated.chat.CitationVO
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.service.LifeCycleAware

interface TradeChatMessagesServiceFacade : LifeCycleAware {
    suspend fun sendChatMessage(
        text: String,
        citationVO: CitationVO?,
    ): Result<Unit>

    suspend fun addChatMessageReaction(
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit>

    suspend fun removeChatMessageReaction(
        messageId: String,
        reactionVO: BisqEasyOpenTradeMessageReactionVO,
    ): Result<Boolean>
}
