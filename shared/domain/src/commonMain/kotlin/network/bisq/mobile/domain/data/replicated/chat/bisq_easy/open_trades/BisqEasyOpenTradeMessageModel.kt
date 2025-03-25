package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport

class BisqEasyOpenTradeMessageModel(
    bisqEasyOpenTradeMessage: BisqEasyOpenTradeMessageDto,
    citationAuthorUserProfile: UserProfileVO?,
    myUserProfile: UserProfileVO,
    chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO>
) {
    // Delegates of BisqEasyOpenTradeMessageDto
    val id: String = bisqEasyOpenTradeMessage.id
    val chatChannelDomain: ChatChannelDomainEnum = bisqEasyOpenTradeMessage.chatChannelDomain
    val channelId: String = bisqEasyOpenTradeMessage.channelId
    val text: String? = bisqEasyOpenTradeMessage.text
    val citation: CitationVO? = bisqEasyOpenTradeMessage.citation
    val date: Long = bisqEasyOpenTradeMessage.date
    val wasEdited: Boolean = bisqEasyOpenTradeMessage.wasEdited
    val chatMessageType: ChatMessageTypeEnum = bisqEasyOpenTradeMessage.chatMessageType
    val receiverUserProfileId: String = bisqEasyOpenTradeMessage.receiverUserProfileId
    val senderUserProfile: UserProfileVO = bisqEasyOpenTradeMessage.senderUserProfile
    val senderUserProfileId = senderUserProfile.id
    val receiverNetworkId: NetworkIdVO = bisqEasyOpenTradeMessage.receiverNetworkId
    val tradeId: String = bisqEasyOpenTradeMessage.tradeId
    val mediator: UserProfileVO? = bisqEasyOpenTradeMessage.mediator
    val bisqEasyOffer: BisqEasyOfferVO? = bisqEasyOpenTradeMessage.bisqEasyOffer


    val textString: String = text ?: ""

    // Used for protocol log message
    var decodedText: String = text?.let { I18nSupport.decode(it) } ?: ""

    val dateString: String = DateUtils.toDateTime(date)


    val senderUserName = senderUserProfile.userName

    val myUserName = myUserProfile.userName
    val myUserProfileId = myUserProfile.id

    val citationAuthorUserName = citationAuthorUserProfile?.userName
    val citationAuthorUserProfileId = myUserProfile.id

    val citationString: String = citation?.text ?: ""

    val isMyMessage: Boolean = senderUserProfileId == myUserProfileId

    val _reactions: MutableStateFlow<List<BisqEasyOpenTradeMessageReactionVO>> = MutableStateFlow(chatMessageReactions)
    val reactions: StateFlow<List<BisqEasyOpenTradeMessageReactionVO>> = _reactions

    fun setReactions(chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO>) {
        _reactions.value = chatMessageReactions
    }

    fun isMyChatReaction(reaction: BisqEasyOpenTradeMessageReactionVO): Boolean {
        return myUserProfileId == reaction.senderUserProfile.id
    }
}
