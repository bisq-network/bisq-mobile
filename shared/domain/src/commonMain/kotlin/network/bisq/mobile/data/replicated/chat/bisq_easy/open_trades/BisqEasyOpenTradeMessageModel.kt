package network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.CitationVO
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport

class BisqEasyOpenTradeMessageModel(
    private val bisqEasyOpenTradeMessage: BisqEasyOpenTradeMessageDto,
    myUserProfile: UserProfileVO,
    chatReactions: List<BisqEasyOpenTradeMessageReactionVO>,
) {
    val senderUserProfile: UserProfileVO get() = bisqEasyOpenTradeMessage.senderUserProfile
    private val myUserProfileId = myUserProfile.id

    private val _chatReactions: MutableStateFlow<List<BisqEasyOpenTradeMessageReactionVO>> =
        MutableStateFlow(
            chatReactions,
        )
    val chatReactions: StateFlow<List<BisqEasyOpenTradeMessageReactionVO>> = _chatReactions.asStateFlow()

    // Delegates of BisqEasyOpenTradeMessageDto
    val id: String get() = bisqEasyOpenTradeMessage.messageId
    val text: String? get() = bisqEasyOpenTradeMessage.text
    val citation: CitationVO? get() = bisqEasyOpenTradeMessage.citation
    val date: Long get() = bisqEasyOpenTradeMessage.date
    val chatMessageType: ChatMessageTypeEnum get() = bisqEasyOpenTradeMessage.chatMessageType
    val tradeId: String get() = bisqEasyOpenTradeMessage.tradeId
    val mediator: UserProfileVO? get() = bisqEasyOpenTradeMessage.mediator
    val bisqEasyOffer: BisqEasyOfferVO? get() = bisqEasyOpenTradeMessage.bisqEasyOffer
    val citationAuthorUserName get() = bisqEasyOpenTradeMessage.citationAuthorUserProfile?.userName

    val textString: String get() = text ?: ""

    // Used for protocol log message
    val decodedText: String get() = text?.let { I18nSupport.decode(it) } ?: ""

    val dateString: String get() = DateUtils.toDateTime(date)
    val senderUserProfileId get() = senderUserProfile.id
    val senderUserName get() = senderUserProfile.userName
    val citationString: String get() = citation?.text ?: ""
    val isMyMessage: Boolean get() = senderUserProfileId == myUserProfileId

    private val _messageDeliveryStatus = MutableStateFlow<Map<String, MessageDeliveryInfoVO>>(emptyMap())
    val messageDeliveryStatus = _messageDeliveryStatus.asStateFlow()

    fun isMyChatReaction(reaction: BisqEasyOpenTradeMessageReactionVO): Boolean = myUserProfileId == reaction.senderUserProfile.id

    fun setReactions(chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO>) {
        _chatReactions.value = chatMessageReactions
    }

    fun removeMessageDeliveryStatusObserver(messageDeliveryServiceFacade: MessageDeliveryServiceFacade) {
        messageDeliveryServiceFacade.removeMessageDeliveryStatusObserver(bisqEasyOpenTradeMessage.messageId)
    }

    fun addMessageDeliveryStatusObserver(messageDeliveryServiceFacade: MessageDeliveryServiceFacade) {
        messageDeliveryServiceFacade.addMessageDeliveryStatusObserver(bisqEasyOpenTradeMessage.messageId) { entry ->
            _messageDeliveryStatus.update { it + entry }
        }
    }
}
