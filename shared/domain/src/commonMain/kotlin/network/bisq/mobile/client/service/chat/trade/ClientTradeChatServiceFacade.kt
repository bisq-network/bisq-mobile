package network.bisq.mobile.client.service.chat.trade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientTradeChatServiceFacade(
    private val tradesServiceFacade: TradesServiceFacade,
    private val apiGateway: TradeChatApiGateway,
    private val json: Json
) : TradeChatServiceFacade, Logging {
    // Properties
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    // Misc
    private var active = false
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var jobs: MutableSet<Job> = mutableSetOf()
    private val _allBisqEasyOpenTradeMessages: MutableStateFlow<Set<BisqEasyOpenTradeMessageModel>> = MutableStateFlow(emptySet())
    val allBisqEasyOpenTradeMessages: StateFlow<Set<BisqEasyOpenTradeMessageModel>> = _allBisqEasyOpenTradeMessages

    override fun activate() {
        if (active) {
            log.w { "deactivating first" }
            deactivate()
        }

        jobs += coroutineScope.launch {
            allBisqEasyOpenTradeMessages.collect { _ -> applyChatMessages() }
        }
        jobs += coroutineScope.launch {
            selectedTrade.collect { _ -> applyChatMessages() }
        }
        jobs += coroutineScope.launch {
            subscribeTradeChats()
        }

        active = true
    }

    override fun deactivate() {
        if (!active) {
            log.w { "Skipping deactivation as its already deactivated" }
            return
        }

        jobs.map { it.cancel() }
        jobs.clear()

        active = false
    }

    private suspend fun subscribeTradeChats() {
        val observer = apiGateway.subscribeTradeChats()
        observer.webSocketEvent.collect { webSocketEvent ->
            val deferredPayload = webSocketEvent?.deferredPayload
            if (deferredPayload == null) {
                return@collect
            }
            val webSocketEventPayload: WebSocketEventPayload<List<BisqEasyOpenTradeMessageDto>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            val payload = webSocketEventPayload.payload
            val messages = payload.map { toBisqEasyOpenTradeMessageModel(it) }.toSet()
            _allBisqEasyOpenTradeMessages.value = _allBisqEasyOpenTradeMessages.value + messages
        }
    }

    private fun ClientTradeChatServiceFacade.applyChatMessages() {
        val bisqEasyOpenTradeChannelModel = selectedTrade.value?.bisqEasyOpenTradeChannelModel
        val messages = allBisqEasyOpenTradeMessages.value.filter { it.tradeId == bisqEasyOpenTradeChannelModel?.tradeId }.toSet()
        bisqEasyOpenTradeChannelModel?.addAllChatMessages(messages)
    }

    private fun toBisqEasyOpenTradeMessageModel(bisqEasyOpenTradeMessageDto: BisqEasyOpenTradeMessageDto): BisqEasyOpenTradeMessageModel {
        val citationAuthorUserProfile: UserProfileVO = bisqEasyOpenTradeMessageDto.senderUserProfile
        val chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO> = bisqEasyOpenTradeMessageDto.chatMessageReactions.toList()

        val myUserProfile: UserProfileVO = bisqEasyOpenTradeMessageDto.senderUserProfile //todo
        return BisqEasyOpenTradeMessageModel(
            bisqEasyOpenTradeMessageDto,
            citationAuthorUserProfile,
            myUserProfile,
            chatMessageReactions
        )
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