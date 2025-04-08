package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

@Serializable
data class BisqEasyOpenTradeMessageDto(
    val tradeId: String,
    val messageId: String,
    val channelId: String,
    val senderUserProfile: UserProfileVO,
    val receiverUserProfileId: String,
    val receiverNetworkId: NetworkIdVO,
    val text: String?,
    val citation: CitationVO?,
    val date: Long,
    val mediator: UserProfileVO?,
    val chatMessageType: ChatMessageTypeEnum,
    val bisqEasyOffer: BisqEasyOfferVO?,
    val chatMessageReactions: Set<BisqEasyOpenTradeMessageReactionVO>,
    val citationAuthorUserProfile: UserProfileVO?
)