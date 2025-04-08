package network.bisq.mobile.android.node.service.chat.trade

import bisq.chat.ChatMessageType
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.mapping.Mappings.BisqEasyOpenTradeMessageReactionMapping
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

// When we add other chat types we will refactor that class to provide a base class for the common areas.
class NodeTradeChatMessagesServiceFacade(
    applicationService: AndroidApplicationService.Provider,
    private val tradesServiceFacade: TradesServiceFacade
) : TradeChatMessagesServiceFacade, Logging {

    // Dependencies
    private val bisqEasyOpenTradeChannelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }

    // Properties
    private val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade
    private val openTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = tradesServiceFacade.openTradeItems

    // Misc
    private var active = false
    private var channelsPin: Pin? = null
    private val reactionsPinByMessageId: MutableMap<String, Pin> = mutableMapOf()
    private val pinsByTradeId: MutableMap<String, MutableSet<Pin>> = mutableMapOf()

    override fun activate() {
        if (active) {
            log.w { "deactivating first" }
            deactivate()
        }

        channelsPin = bisqEasyOpenTradeChannelService.channels.addObserver(object : CollectionObserver<BisqEasyOpenTradeChannel?> {
            override fun add(channel: BisqEasyOpenTradeChannel?) {
                if (channel != null) {
                    handleChannelAdded(channel)
                }
            }

            override fun remove(element: Any) {
                if (element is BisqEasyOpenTradeChannel) {
                    handleChannelRemoved(element)
                }
            }

            override fun clear() {
                handleChannelsCleared()
            }
        })

        active = true
    }

    override fun deactivate() {
        if (!active) {
            log.w { "Skipping deactivation as its already deactivated" }
            return
        }
        channelsPin?.unbind()

        unbindAllReactionsPins()
        unbindAllPinsByTradeId()

        active = false
    }

    override suspend fun sendChatMessage(text: String, citationVO: CitationVO?): Result<Unit> {
        selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id.let { id ->
            val citation = Optional.ofNullable(citationVO?.let { Mappings.CitationMapping.toBisq2Model(it) })
            val channel = bisqEasyOpenTradeChannelService.findChannel(id).get()
            bisqEasyOpenTradeChannelService.sendTextMessage(text, citation, channel)
        }
        return Result.success(Unit)
    }

    override suspend fun addChatMessageReaction(messageId: String, reactionEnum: ReactionEnum): Result<Unit> {
        return addOrRemoveChatMessageReaction(messageId, reactionEnum, false)
    }

    override suspend fun removeChatMessageReaction(messageId: String, reactionVO: BisqEasyOpenTradeMessageReactionVO): Result<Boolean> {
        return if (userIdentityService.findUserIdentity(reactionVO.senderUserProfile.id).isPresent) {
            val reaction = ReactionEnum.entries[reactionVO.reactionId]
            val result = addOrRemoveChatMessageReaction(messageId, reaction, true)
            if (result.isSuccess) {
                Result.success(true)
            } else {
                throw result.exceptionOrNull()!!
            }
        } else {
            // Not our reaction, so we cannot remove it
            Result.success(false)
        }
    }


    // Private
    private fun handleChannelAdded(channel: BisqEasyOpenTradeChannel) {
        val tradeId = channel.tradeId
        pinsByTradeId[tradeId]?.forEach { it.unbind() }
        val pins = mutableSetOf<Pin>()
        pinsByTradeId[tradeId] = pins

        unbindAllReactionsPins()
        pins += channel.chatMessages.addObserver(object : CollectionObserver<BisqEasyOpenTradeMessage> {
            override fun add(message: BisqEasyOpenTradeMessage) {
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
                    val pin = message.chatMessageReactions.addObserver {
                        openTradeItem.bisqEasyOpenTradeChannelModel.chatMessages.value.find { messageId == it.id }?.let { model ->
                            val chatMessageReactions = message.chatMessageReactions
                                .filter { !it.isRemoved }
                                .map { reaction -> BisqEasyOpenTradeMessageReactionMapping.fromBisq2Model(reaction) }
                            model.setReactions(chatMessageReactions)
                        }
                    }
                    reactionsPinByMessageId[messageId] to pin
                }

                val citationAuthorUserProfile: UserProfile? =
                    message.citation.flatMap { citation -> userProfileService.findUserProfile(citation.authorUserProfileId) }
                        .orElse(null)
                val myUserProfile = userIdentityService.selectedUserIdentity.userProfile
                val model: BisqEasyOpenTradeMessageModel = Mappings.BisqEasyOpenTradeMessageModelMapping.fromBisq2Model(
                    message,
                    citationAuthorUserProfile,
                    myUserProfile
                )
                openTradeItem.bisqEasyOpenTradeChannelModel.addChatMessages(model)
            }

            override fun remove(element: Any) {
                // Private messages cannot be removed
            }

            override fun clear() {
            }
        })
    }

    private fun handleChannelRemoved(channel: BisqEasyOpenTradeChannel) {
        unbindPinByTradeId(channel.tradeId)
        unbindAllReactionsPins()
    }

    private fun handleChannelsCleared() {
        unbindAllPinsByTradeId()
        unbindAllReactionsPins()
    }

    private fun addOrRemoveChatMessageReaction(messageId: String, reactionEnum: ReactionEnum, isRemoved: Boolean): Result<Unit> {
        selectedTrade.value?.bisqEasyOpenTradeChannelModel?.id.let { id ->
            bisqEasyOpenTradeChannelService.findChannel(id).getOrNull()?.let { channel ->
                channel.chatMessages.find { it.id == messageId }?.let { message ->
                    val reaction = Mappings.ReactionMapping.toBisq2Model(reactionEnum)
                    bisqEasyOpenTradeChannelService.sendTextMessageReaction(message, channel, reaction, isRemoved)
                }
            }
        }
        return Result.success(Unit)
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