package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id

//todo will get completed with work on chat
class BisqEasyOpenTradeChannelModel(bisqEasyOpenTradeChannelDto: BisqEasyOpenTradeChannelDto) {
    // Delegates of bisqEasyOpenTradeChannelVO
    val id: String = bisqEasyOpenTradeChannelDto.id
    val tradeId: String = bisqEasyOpenTradeChannelDto.tradeId
    val bisqEasyOffer: BisqEasyOfferVO = bisqEasyOpenTradeChannelDto.bisqEasyOffer
    val myUserIdentity: UserIdentityVO = bisqEasyOpenTradeChannelDto.myUserIdentity
    val traders: Set<UserProfileVO> = bisqEasyOpenTradeChannelDto.traders
    val mediator: UserProfileVO? = bisqEasyOpenTradeChannelDto.mediator

    // Mutable properties
    val isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val chatMessages: MutableSet<BisqEasyOpenTradeMessageModel> = mutableSetOf()
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
                // optionalMediatorPostfix= ", " + mediator.userName + " (" + Res.get("bisqEasy.mediator")+ ")"
                optionalMediatorPostfix = ", " + mediator.userName + " (Mediator)"
            } else {
                optionalMediatorPostfix = ""
            }
            return "$shortOfferId: $peer$optionalMediatorPostfix"
        }
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
}