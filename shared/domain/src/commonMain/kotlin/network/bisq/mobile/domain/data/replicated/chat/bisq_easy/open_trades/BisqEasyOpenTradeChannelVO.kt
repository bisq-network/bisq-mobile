package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

@Serializable
data class BisqEasyOpenTradeChannelVO(
    val id: String,
    val tradeId: String,
    val bisqEasyOffer: BisqEasyOfferVO,
    val myUserIdentity: UserIdentityVO,
    val traders: Set<UserProfileVO>,
    val mediator: UserProfileVO?,
)