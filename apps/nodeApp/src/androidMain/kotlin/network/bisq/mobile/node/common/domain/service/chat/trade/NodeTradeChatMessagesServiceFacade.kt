package network.bisq.mobile.node.common.domain.service.chat.trade

import bisq.chat.ChatMessageType
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.chat.CitationVO
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

// When we add other chat types we will refactor that class to provide a base class for the common areas.
class NodeTradeChatMessagesServiceFacade(
    applicationService: AndroidApplicationService.Provider,
    private val tradesServiceFacade: TradesServiceFacade,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade,
) : ServiceFacade(),
    TradeChatMessagesServiceFacade {
    companion object {
        // Bisq2's RateLimitedPersistenceClient rate-limits persist() to 1,000 ms between writes.
        // PROTOCOL_LOG_MESSAGE arrives ~200 ms after TAKE_BISQ_EASY_OFFER, so its persist() is
        // always dropped. 1,200 ms safely clears the window before we force a flush.
        internal const val PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS = 1_200L
    }

    // Dependencies
    private val bisqEasyOpenTradeChannelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }

    // Properties
    private val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade
    private val openTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = tradesServiceFacade.openTradeItems

    // Misc
    private var channelsPin: Pin? = null
    private val reactionsPinByMessageId: MutableMap<String, Pin> = mutableMapOf()
    private val pinsByTradeId: MutableMap<String, MutableSet<Pin>> = mutableMapOf()

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        channelsPin =
            bisqEasyOpenTradeChannelService.channels.addObserver(
                object : CollectionObserver<BisqEasyOpenTradeChannel> {
                    override fun onAdded(channel: BisqEasyOpenTradeChannel) {
                        handleChannelAdded(channel)
                    }

                    override fun onRemoved(element: Any) {
                        if (element is BisqEasyOpenTradeChannel) {
                            handleChannelRemoved(element)
                        }
                    }

                    override fun onCleared() {
                        handleChannelsCleared()
                    }
                },
            )
    }

    override suspend fun deactivate() {
        channelsPin?.unbind()

        unbindAllReactionsPins()
        unbindAllPinsByTradeId()

        super<ServiceFacade>.deactivate()
    }

    override suspend fun sendChatMessage(
        text: String,
        citationVO: CitationVO?,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id.let { id ->
                val citation =
                    Optional.ofNullable(citationVO?.let { Mappings.CitationMapping.toBisq2Model(it) })
                val channel = bisqEasyOpenTradeChannelService.findChannel(id).get()
                bisqEasyOpenTradeChannelService.sendTextMessage(text, citation, channel)
            }
            Result.success(Unit)
        }

    override suspend fun addChatMessageReaction(
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> = addOrRemoveChatMessageReaction(messageId, reactionEnum, false)

    override suspend fun removeChatMessageReaction(
        messageId: String,
        reactionVO: BisqEasyOpenTradeMessageReactionVO,
    ): Result<Boolean> =
        if (userIdentityService.findUserIdentity(reactionVO.senderUserProfile.id).isPresent) {
            val reaction = ReactionEnum.entries[reactionVO.reactionId]
            val result = addOrRemoveChatMessageReaction(messageId, reaction, true)
            if (result.isSuccess) {
                Result.success(true)
            } else {
                throw result.exceptionOrNull() ?: IllegalStateException("No Exception is set in result failure")
            }
        } else {
            // Not our reaction, so we cannot remove it
            Result.success(false)
        }

    // Private
    private fun handleChannelAdded(channel: BisqEasyOpenTradeChannel) {
        val tradeId = channel.tradeId
        pinsByTradeId[tradeId]?.forEach { it.unbind() }
        val pins = mutableSetOf<Pin>()
        pinsByTradeId[tradeId] = pins

        unbindAllReactionsPins()
        pins +=
            channel.chatMessages.addObserver(
                object : CollectionObserver<BisqEasyOpenTradeMessage> {
                    // INVARIANT: persist=false/onAllAdded vs persist=true/onAdded assumes Bisq2 replays
                    // existing messages only in onAllAdded; live messages always arrive via onAdded.
                    override fun onAllAdded(values: Collection<out BisqEasyOpenTradeMessage>) {
                        // Override the default (which calls onAdded per element) solely to pass
                        // persist=false, preventing a delayed persist job from being scheduled
                        // for every historical PROTOCOL_LOG_MESSAGE replayed at startup.
                        values.forEach { message ->
                            addMessageToModel(tradeId, message, persist = false)
                        }
                    }

                    override fun onAdded(message: BisqEasyOpenTradeMessage) {
                        addMessageToModel(tradeId, message, persist = true)
                    }

                    override fun onRemoved(element: Any) {
                        // Private messages cannot be removed
                    }

                    override fun onCleared() {
                    }
                },
            )
    }

    /**
     * Adds a Bisq2 chat message to the presentation model.
     *
     * [ChatMessageType.TAKE_BISQ_EASY_OFFER] is always filtered — it is a protocol contract
     * carrier with no user-visible text; [ChatMessageType.PROTOCOL_LOG_MESSAGE] is the display
     * message. When [persist] is true (live messages) a delayed persist is scheduled so
     * PROTOCOL_LOG_MESSAGE reaches disk despite Bisq2's rate-limited persist() dropping it.
     * [persist] = false (startup replay) skips the persist — data is already on disk.
     */
    private fun addMessageToModel(
        tradeId: String,
        message: BisqEasyOpenTradeMessage,
        persist: Boolean,
    ) {
        if (message.chatMessageType == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
            return
        }
        val openTradeItem = openTradeItems.value.find { it.tradeId == tradeId }
        if (openTradeItem == null) {
            log.w { "We got called handleChannelAdded but we have not found any trade list item with tradeId $tradeId" }
            return
        }

        val messageId = message.id
        if (!reactionsPinByMessageId.containsKey(messageId)) {
            val pin =
                message.chatMessageReactions.addObserver {
                    openTradeItem.bisqEasyOpenTradeChannelModel.chatMessages.value
                        .find { messageId == it.id }
                        ?.let { model ->
                            val chatMessageReactions =
                                message.chatMessageReactions
                                    .filter { !it.isRemoved }
                                    .map { reaction ->
                                        Mappings.BisqEasyOpenTradeMessageReactionMapping.fromBisq2Model(
                                            reaction,
                                        )
                                    }
                            model.setReactions(chatMessageReactions)
                        }
                }
            reactionsPinByMessageId[messageId] = pin
        }

        val citationAuthorUserProfile: UserProfile? =
            message.citation
                .flatMap { citation -> userProfileService.findUserProfile(citation.authorUserProfileId) }
                .orElse(null)
        val myUserProfile = userIdentityService.selectedUserIdentity.userProfile
        val model: BisqEasyOpenTradeMessageModel =
            Mappings.BisqEasyOpenTradeMessageModelMapping.fromBisq2Model(
                message,
                citationAuthorUserProfile,
                myUserProfile,
            )
        openTradeItem.bisqEasyOpenTradeChannelModel.addChatMessages(model)

        // PROTOCOL_LOG_MESSAGE's persist() is always rate-limited (arrives ~200 ms after
        // TAKE_BISQ_EASY_OFFER, inside the 1 000 ms window). Force a persist once the window clears.
        if (persist && message.chatMessageType == ChatMessageType.PROTOCOL_LOG_MESSAGE) {
            serviceScope.launch(Dispatchers.Default) {
                delay(PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS)
                bisqEasyOpenTradeChannelService.persist()
            }
        }
    }

    private fun handleChannelRemoved(channel: BisqEasyOpenTradeChannel) {
        unbindPinByTradeId(channel.tradeId)
        unbindAllReactionsPins()
    }

    private fun handleChannelsCleared() {
        unbindAllPinsByTradeId()
        unbindAllReactionsPins()
    }

    private suspend fun addOrRemoveChatMessageReaction(
        messageId: String,
        reactionEnum: ReactionEnum,
        isRemoved: Boolean,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id.let { id ->
                bisqEasyOpenTradeChannelService.findChannel(id).getOrNull()?.let { channel ->
                    channel.chatMessages.find { it.id == messageId }?.let { message ->
                        val reaction = Mappings.ReactionMapping.toBisq2Model(reactionEnum)
                        bisqEasyOpenTradeChannelService.sendTextMessageReaction(
                            message,
                            channel,
                            reaction,
                            isRemoved,
                        )
                    }
                }
            }
            Result.success(Unit)
        }

    private fun unbindPinByTradeId(tradeId: String) {
        if (pinsByTradeId.containsKey(tradeId)) {
            pinsByTradeId[tradeId]?.forEach { it.unbind() }
            pinsByTradeId.remove(tradeId)
        }
    }

    private fun unbindAllPinsByTradeId() {
        pinsByTradeId.values.forEach { pins -> pins.forEach { it.unbind() } }
        pinsByTradeId.clear()
    }

    private fun unbindAllReactionsPins() {
        reactionsPinByMessageId.values.forEach { it.unbind() }
        reactionsPinByMessageId.clear()
    }
}
