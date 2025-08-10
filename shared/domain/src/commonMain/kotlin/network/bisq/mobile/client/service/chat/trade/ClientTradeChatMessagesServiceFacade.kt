package network.bisq.mobile.client.service.chat.trade

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade

class ClientTradeChatMessagesServiceFacade(
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val apiGateway: TradeChatMessagesApiGateway,
    private val json: Json
) : ServiceFacade(), TradeChatMessagesServiceFacade {

    // Properties
    private val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade
    private val selectedUserProfileId: StateFlow<UserProfileVO?> get() = userProfileServiceFacade.selectedUserProfile

    private val _allBisqEasyOpenTradeMessages: MutableStateFlow<Set<BisqEasyOpenTradeMessageDto>> =
        MutableStateFlow(emptySet())
    private val allBisqEasyOpenTradeMessages: StateFlow<Set<BisqEasyOpenTradeMessageDto>> get() =
        _allBisqEasyOpenTradeMessages.asStateFlow()

    private val _allChatReactions: MutableStateFlow<Set<BisqEasyOpenTradeMessageReactionVO>> =
        MutableStateFlow(emptySet())
    private val allChatReactions: StateFlow<Set<BisqEasyOpenTradeMessageReactionVO>> get() = _allChatReactions.asStateFlow()

    private var jobs: MutableSet<Job> = mutableSetOf()

    // Misc
    override fun activate() {
        super<ServiceFacade>.activate()

        jobs.add(serviceScope.launch(Dispatchers.Default) {
            selectedTrade.collect {
                if (it != null) {
                    updateChatMessages(tradeId = it.tradeId)
                }
            }
        })
        jobs.add(serviceScope.launch(Dispatchers.Default) {
            selectedUserProfileId.collect { _ ->
                if (selectedTrade.value != null) {
                    updateChatMessages(tradeId = selectedTrade.value!!.tradeId)
                }
            }
        })

        jobs.add(serviceScope.launch {
            subscribeTradeChats()
        })
        jobs.add(serviceScope.launch {
            subscribeChatReactions()
        })
    }

    override fun deactivate() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        super<ServiceFacade>.deactivate()
    }

    private suspend fun subscribeTradeChats() {
        val observer = apiGateway.subscribeTradeChats()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            val webSocketEventPayload: WebSocketEventPayload<List<BisqEasyOpenTradeMessageDto>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            val payload = webSocketEventPayload.payload
            _allBisqEasyOpenTradeMessages.update { it + payload }
            // To update bisqEasyOpenTradeChannelModel of the trades
            val updatedTradeIds = payload.map { it.tradeId }.toSet()
            updatedTradeIds.forEach { tradeId ->
                updateChatMessages(tradeId)
            }
        }
    }

    private suspend fun subscribeChatReactions() {
        val observer = apiGateway.subscribeChatReactions()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
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
                        .filter {
                            it.chatMessageId == reaction.chatMessageId &&
                                    it.senderUserProfile.id == reaction.senderUserProfile.id &&
                                    it.reactionId == reaction.reactionId
                        }
                        .let { toRemove ->
                            _allChatReactions.update { it - toRemove.toSet() }
                        }
                } else {
                    _allChatReactions.update { it + reaction }
                }
                // To update bisqEasyOpenTradeChannelModel of the trades
                try {
                    val tradeId = _allBisqEasyOpenTradeMessages.value.find {
                        it.messageId == reaction.chatMessageId
                    }?.tradeId
                    if (tradeId == null) {
                        log.d { "No message found for reaction: messageId=${reaction.chatMessageId}" }
                    } else {
                        updateChatMessages(tradeId = tradeId)
                    }
                } catch (e: Exception) {
                    log.e { "Error while parsing reaction ${reaction.id}: $e" }
                }
            }
        }
    }

    private fun updateChatMessages(tradeId: String) {
        val myUserProfile = selectedUserProfileId.value ?: return
        val bisqEasyOpenTradeChannelModel = tradesServiceFacade.openTradeItems.value
            .find { it.tradeId == tradeId }
            ?.bisqEasyOpenTradeChannelModel ?: return
        val messages = allBisqEasyOpenTradeMessages.value
            .asSequence()
            .filter { it.tradeId == tradeId }
            .map { message ->
                val chatReactions =
                    allChatReactions.value.filter { it.chatMessageId == message.messageId && !it.isRemoved }
                BisqEasyOpenTradeMessageModel(message, myUserProfile, chatReactions)
            }
            .toSet()
        bisqEasyOpenTradeChannelModel.setAllChatMessages(messages)
    }

    override suspend fun sendChatMessage(text: String, citationVO: CitationVO?): Result<Unit> {
        require(selectedTrade.value != null)
        selectedTrade.value!!.bisqEasyOpenTradeChannelModel.id.let { channelId ->
            val apiResult = apiGateway.sendTextMessage(channelId, text, citationVO)
            if (apiResult.isSuccess) {
                return Result.success(Unit)
            } else {
                return Result.failure(apiResult.exceptionOrNull()!!)
            }
        }
    }

    override suspend fun addChatMessageReaction(messageId: String, reactionEnum: ReactionEnum): Result<Unit> {
        require(selectedTrade.value != null)
        selectedTrade.value!!.bisqEasyOpenTradeChannelModel.id.let { channelId ->
            val apiResult = apiGateway.addChatMessageReaction(channelId, messageId, reactionEnum)
            if (apiResult.isSuccess) {
                return Result.success(Unit)
            } else {
                return Result.failure(apiResult.exceptionOrNull()!!)
            }
        }
    }

    // Returns true if we could remove the reaction (if it was created by ourself)
    override suspend fun removeChatMessageReaction(
        messageId: String,
        reactionVO: BisqEasyOpenTradeMessageReactionVO
    ): Result<Boolean> {
        require(selectedTrade.value != null)
        selectedTrade.value!!.bisqEasyOpenTradeChannelModel.id.let { channelId ->
            val apiResult = apiGateway.removeChatMessageReaction(channelId, messageId, reactionVO)
            if (apiResult.isSuccess) {
                return Result.success(true)
            } else {
                return Result.failure(apiResult.exceptionOrNull()!!)
            }
        }
    }
}