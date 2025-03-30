package network.bisq.mobile.client.service.chat.trade

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientTradeChatServiceFacade(val tradesServiceFacade: TradesServiceFacade) : TradeChatServiceFacade, Logging {
    // Properties
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade


    // Misc
    private var active = false

    override fun activate() {
        if (active) {
            log.w { "deactivating first" }
            deactivate()
        }

        active = true
    }

    override fun deactivate() {
        if (!active) {
            log.w { "Skipping deactivation as its already deactivated" }
            return
        }

        active = false
    }


    override suspend fun sendChatMessage(text: String, citationVO: CitationVO?): Result<Unit> {
        /*selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id.let { id ->
            val citation = Optional.ofNullable(citationVO?.let { Mappings.CitationMapping.toBisq2Model(it) })
            val channel = bisqEasyOpenTradeChannelService.findChannel(id).get()
            bisqEasyOpenTradeChannelService.sendTextMessage(text, citation, channel)
        }*/
        return Result.success(Unit)
    }

    override suspend fun addChatMessageReaction(messageId: String, reactionEnum: ReactionEnum): Result<Unit> {
        return addOrRemoveChatMessageReaction(messageId, reactionEnum, false)
    }

    override suspend fun removeChatMessageReaction(messageId: String, reactionVO: BisqEasyOpenTradeMessageReactionVO): Result<Unit> {
        /*if (userIdentityService.findUserIdentity(reactionVO.senderUserProfile.id).isPresent) {
            val reaction = ReactionEnum.entries[reactionVO.reactionId]
            return addOrRemoveChatMessageReaction(messageId, reaction, true)
        } else {
            // Not our reaction, so we cannot remove it
            return Result.success(Unit)
        }*/

        return Result.success(Unit)
    }

    private fun addOrRemoveChatMessageReaction(
        messageId: String, reactionEnum: ReactionEnum, isRemoved: Boolean
    ): Result<Unit> {
        /* selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id.let { id ->
             bisqEasyOpenTradeChannelService.findChannel(id).getOrNull()?.let { channel ->
                 channel.chatMessages.find { it.id == messageId }?.let { message ->
                     val reaction = Mappings.ReactionMapping.toBisq2Model(reactionEnum)
                     bisqEasyOpenTradeChannelService.sendTextMessageReaction(message, channel, reaction, isRemoved)
                 }
             }
         }*/
        return Result.success(Unit)
    }


}