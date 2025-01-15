package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

//todo will get completed with work on chat
//todo missing dto on Bisq 2 side, missing fields for initial value of mutable data
@Serializable
data class BisqEasyOpenTradeChannelDto(
    val id: String,
    val tradeId: String,
    val bisqEasyOffer: BisqEasyOfferVO,
    val myUserIdentity: UserIdentityVO,
    val traders: Set<UserProfileVO>,
    val mediator: UserProfileVO?,
)