package network.bisq.mobile.node.common.domain.service.chat.trade

import bisq.chat.ChatMessageType
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.resultCatching
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.mapping.chat.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel as Bisq2BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage as Bisq2BisqEasyOpenTradeMessage

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
                object : CollectionObserver<Bisq2BisqEasyOpenTradeChannel> {
                    override fun onAdded(channel: Bisq2BisqEasyOpenTradeChannel) {
                        handleChannelAdded(channel)
                    }

                    override fun onRemoved(element: Any) {
                        if (element is Bisq2BisqEasyOpenTradeChannel) {
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
        citation: Citation?,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            // `findChannel` is a Java method, so a null id would pass through as a platform type and
            // the failure would surface as an empty Optional deep inside Bisq2. Both misses are
            // turned into a failed Result here instead, because the caller does act on the
            // difference: `TradeChatPresenter` clears the quoted message on success only.
            val channelId =
                selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id
                    ?: return@withContext Result.failure(IllegalStateException("No trade is selected"))
            val channel =
                bisqEasyOpenTradeChannelService.findChannel(channelId).getOrNull()
                    ?: return@withContext Result.failure(IllegalStateException("No channel found for the selected trade"))
            val bisq2Citation =
                Optional.ofNullable(citation?.let { Mappings.CitationMapping.toBisq2Model(it) })
            // Bisq2 hands back a CompletableFuture; dropping it would report every send as delivered.
            // The client facade propagates the dispatch failure, and the delivery-status UI only
            // covers what happens after dispatch, so this is the only place the node mode can.
            resultCatching { bisqEasyOpenTradeChannelService.sendTextMessage(text, bisq2Citation, channel).await() }
                .map { }
        }

    override suspend fun addChatMessageReaction(
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> = addOrRemoveChatMessageReaction(messageId, reactionEnum, false)

    override suspend fun removeChatMessageReaction(
        messageId: String,
        reaction: BisqEasyOpenTradeMessageReaction,
    ): Result<Boolean> =
        if (userIdentityService.findUserIdentity(reaction.senderUserProfile.id).isPresent) {
            val reactionEnum = ReactionEnum.entries[reaction.reactionId]
            val result = addOrRemoveChatMessageReaction(messageId, reactionEnum, true)
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
    private fun handleChannelAdded(channel: Bisq2BisqEasyOpenTradeChannel) {
        val tradeId = channel.tradeId
        pinsByTradeId[tradeId]?.forEach { it.unbind() }
        val pins = mutableSetOf<Pin>()
        pinsByTradeId[tradeId] = pins

        unbindAllReactionsPins()
        pins +=
            channel.chatMessages.addObserver(
                object : CollectionObserver<Bisq2BisqEasyOpenTradeMessage> {
                    // INVARIANT: persist=false/onAllAdded vs persist=true/onAdded assumes Bisq2 replays
                    // existing messages only in onAllAdded; live messages always arrive via onAdded.
                    override fun onAllAdded(values: Collection<out Bisq2BisqEasyOpenTradeMessage>) {
                        // Override the default (which calls onAdded per element) solely to pass
                        // persist=false, preventing a delayed persist job from being scheduled
                        // for every historical PROTOCOL_LOG_MESSAGE replayed at startup.
                        values.forEach { message ->
                            addMessageToModel(tradeId, message, persist = false)
                        }
                    }

                    override fun onAdded(message: Bisq2BisqEasyOpenTradeMessage) {
                        addMessageToModel(tradeId, message, persist = true)
                    }

                    override fun onRemoved(element: Any) {
                        // Private messages cannot be removed
                    }

                    override fun onCleared() {
                        // Private messages cannot be removed
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
        message: Bisq2BisqEasyOpenTradeMessage,
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
                                    .map { reaction -> reaction.toDomain() }
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
        val model: BisqEasyOpenTradeMessage =
            message.toDomain(
                citationAuthorUserProfile,
                myUserProfile,
            )
        openTradeItem.bisqEasyOpenTradeChannelModel.addChatMessage(model)

        // PROTOCOL_LOG_MESSAGE's persist() is always rate-limited (arrives ~200 ms after
        // TAKE_BISQ_EASY_OFFER, inside the 1 000 ms window). Force a persist once the window clears.
        if (persist && message.chatMessageType == ChatMessageType.PROTOCOL_LOG_MESSAGE) {
            serviceScope.launch(Dispatchers.Default) {
                delay(PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS)
                bisqEasyOpenTradeChannelService.persist()
            }
        }
    }

    private fun handleChannelRemoved(channel: Bisq2BisqEasyOpenTradeChannel) {
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
