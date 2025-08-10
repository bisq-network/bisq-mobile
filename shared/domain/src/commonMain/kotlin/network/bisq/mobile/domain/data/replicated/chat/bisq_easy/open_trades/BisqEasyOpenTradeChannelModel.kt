package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

//todo will get completed with work on chat
class BisqEasyOpenTradeChannelModel(bisqEasyOpenTradeChannelDto: BisqEasyOpenTradeChannelDto) : Logging {
    // Delegates of bisqEasyOpenTradeChannelVO
    val id: String = bisqEasyOpenTradeChannelDto.id
    val tradeId: String = bisqEasyOpenTradeChannelDto.tradeId
    val bisqEasyOffer: BisqEasyOfferVO = bisqEasyOpenTradeChannelDto.bisqEasyOffer
    val myUserIdentity: UserIdentityVO = bisqEasyOpenTradeChannelDto.myUserIdentity
    val traders: Set<UserProfileVO> = bisqEasyOpenTradeChannelDto.traders
    val mediator: UserProfileVO? = bisqEasyOpenTradeChannelDto.mediator

    // Mutable properties
    private val _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> get() = _isInMediation.asStateFlow()
    private val _chatMessages: MutableStateFlow<Set<BisqEasyOpenTradeMessageModel>> = MutableStateFlow(emptySet())
    val chatMessages: StateFlow<Set<BisqEasyOpenTradeMessageModel>> get() = _chatMessages.asStateFlow()
    val chatChannelNotificationType: MutableStateFlow<ChatChannelNotificationTypeEnum> =
        MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)
    val userProfileIdsOfActiveParticipants: MutableSet<String> = mutableSetOf()
    val numMessagesByAuthorId: MutableMap<String, Int> = mutableMapOf()
    val userProfileIdsOfSendingLeaveMessage: MutableSet<String> = mutableSetOf()

    // Utils
    fun getDisplayString(): String {
        val shortOfferId = bisqEasyOffer.id.substring(0, 4)
        if (isMediator()) {
            require(traders.size == 2) { "traders.size() need to be 2 but is ${traders.size}" }
            val tradersAsList: List<UserProfileVO> = traders.toList()
            return (shortOfferId + ": " + tradersAsList[0].userName) + " - " + tradersAsList[1].userName
        } else {
            val peer: String = getPeer().userName
            val optionalMediatorPostfix: String
            if (mediator != null && isInMediation.value) {
                optionalMediatorPostfix = ", " + mediator.userName + " (" + "bisqEasy.mediator".i18n() + ")"
            } else {
                optionalMediatorPostfix = ""
            }
            return "$shortOfferId: $peer$optionalMediatorPostfix"
        }
    }

    fun setIsMediator(value: Boolean) {
        _isInMediation.value = value
    }

    fun isMediator(): Boolean {
        return mediator != null && mediator.id == myUserIdentity.userProfile.id
    }

    // For the trade peer use case we have only one UserProfileVO in traders.
    // Only for the mediator use case there are the 2 traders, but then getPeer() is not called
    fun getPeer(): UserProfileVO {
        require(traders.size == 1) { "traders is expected to has size 1 at getPeer()" }
        return traders.iterator().next()
    }

    fun addChatMessages(message: BisqEasyOpenTradeMessageModel) {
        _chatMessages.update { it + message }
    }

    fun setAllChatMessages(messages: Set<BisqEasyOpenTradeMessageModel>) {
        _chatMessages.value = messages
    }
}