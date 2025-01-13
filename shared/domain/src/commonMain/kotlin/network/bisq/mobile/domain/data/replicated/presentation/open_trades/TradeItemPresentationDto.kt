package network.bisq.mobile.domain.data.replicated.presentation.open_trades

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO

/**
 * This value object is provided by the backend and is filled with presentation relevant data which are not trivial to provide by
 * the client code. It contains initial values for some mutual fields
 */
@Serializable
data class TradeItemPresentationDto(
    val channel: BisqEasyOpenTradeChannelDto,
    val trade: BisqEasyTradeDto,
    val makerUserProfile: UserProfileVO, // The userName inside userProfile can change when multiple nicknames are in the network
    val takerUserProfile: UserProfileVO, // The userName inside userProfile can change when multiple nicknames are in the network
    val mediatorUserProfile: UserProfileVO?,
    val directionalTitle: String,
    val formattedDate: String,
    val formattedTime: String,
    val market: String,
    val price: Long,
    val formattedPrice: String,
    val baseAmount: Long,
    val formattedBaseAmount: String,
    val quoteAmount: Long,
    val formattedQuoteAmount: String,
    val bitcoinSettlementMethod: String,
    val bitcoinSettlementMethodDisplayString: String,
    val fiatPaymentMethod: String,
    val fiatPaymentMethodDisplayString: String,
    val isFiatPaymentMethodCustom: Boolean,
    val formattedMyRole: String,
    val peersReputationScore: ReputationScoreVO // We do not support updates of reputation score as that happens rather rarely
)