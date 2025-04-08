package network.bisq.mobile.client.service.chat.trade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientTradeChatServiceFacade(
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val apiGateway: TradeChatApiGateway,
    private val json: Json
) : TradeChatServiceFacade, Logging {
    // Properties
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade
    val selectedUserProfileId: StateFlow<UserProfileVO?> get() = userProfileServiceFacade.selectedUserProfile

    private val _allBisqEasyOpenTradeMessages: MutableStateFlow<Set<BisqEasyOpenTradeMessageDto>> = MutableStateFlow(emptySet())
    val allBisqEasyOpenTradeMessages: StateFlow<Set<BisqEasyOpenTradeMessageDto>> = _allBisqEasyOpenTradeMessages

    private val _allChatReactions: MutableStateFlow<Set<BisqEasyOpenTradeMessageReactionVO>> = MutableStateFlow(emptySet())
    val allChatReactions: StateFlow<Set<BisqEasyOpenTradeMessageReactionVO>> = _allChatReactions


    /*  private val _allBisqEasyOpenTradeMessages: MutableStateFlow<Set<BisqEasyOpenTradeMessageModel>> = MutableStateFlow(emptySet())
      val allBisqEasyOpenTradeMessages: StateFlow<Set<BisqEasyOpenTradeMessageModel>> = _allBisqEasyOpenTradeMessages
  */

    // Misc
    private var active = false
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun activate() {
        if (active) {
            log.w { "deactivating first" }
            deactivate()
        }
        jobs += coroutineScope.launch {
            selectedUserProfileId.collect { _ -> updateChatMessages() }
        }
        jobs += coroutineScope.launch {
            selectedTrade.collect { _ -> updateChatMessages() }
        }
        jobs += coroutineScope.launch {
            allBisqEasyOpenTradeMessages.collect { _ -> updateChatMessages() }
        }
        jobs += coroutineScope.launch {
            allChatReactions.collect { _ -> updateChatMessages() }
        }

        jobs += coroutineScope.launch {
            subscribeTradeChats()
        }

        jobs += coroutineScope.launch {
            subscribeChatReactions()
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
            _allBisqEasyOpenTradeMessages.update { it + payload }
        }
    }

    private suspend fun subscribeChatReactions() {
        val observer = apiGateway.subscribeChatReactions()
        observer.webSocketEvent.collect { webSocketEvent ->
            val deferredPayload = webSocketEvent?.deferredPayload
            if (deferredPayload == null) {
                return@collect
            }
            val webSocketEventPayload: WebSocketEventPayload<List<BisqEasyOpenTradeMessageReactionVO>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            val payload = webSocketEventPayload.payload
            payload.forEach { reaction ->
                // We cannot just remove it from the set as the removed reaction has a difference id.
                // We lookup instead the matching reaction and remove that.
                if (reaction.isRemoved) {
                    _allChatReactions.value
                        .filter { it.chatMessageId == reaction.chatMessageId }
                        .filter { it.senderUserProfile.id == reaction.senderUserProfile.id }
                        .filter { it.reactionId == reaction.reactionId }
                        .let { toRemove ->
                            _allChatReactions.update { it - toRemove.toSet() }
                        }
                } else {
                    _allChatReactions.update { it + reaction }
                }
            }
        }
    }

    private fun updateChatMessages() {
        val myUserProfile = selectedUserProfileId.value ?: return
        val bisqEasyOpenTradeChannelModel = selectedTrade.value?.bisqEasyOpenTradeChannelModel
        val messages = allBisqEasyOpenTradeMessages.value
            .filter { it.tradeId == bisqEasyOpenTradeChannelModel?.tradeId }
            .map { message ->
                val chatReactions = allChatReactions.value
                    .filter { it.chatMessageId == message.messageId }
                    .filter { !it.isRemoved }
                BisqEasyOpenTradeMessageModel(message, myUserProfile, chatReactions)
            }.toSet()
        bisqEasyOpenTradeChannelModel?.setAllChatMessages(messages)
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