package network.bisq.mobile.domain.service.chat.trade

import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum

interface TradeChatServiceFacade : LifeCycleAware {

    suspend fun sendChatMessage(text: String, citationVO: CitationVO?): Result<Unit>

    suspend fun addChatMessageReaction(messageId: String, reactionEnum: ReactionEnum): Result<Unit>

    suspend fun removeChatMessageReaction(messageId: String, reactionVO: BisqEasyOpenTradeMessageReactionVO): Result<Unit>
}