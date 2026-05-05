package network.bisq.mobile.data.model.trade

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO

/**
 * Slim wire DTO mirroring the server-side ClosedTradeListItemDto (closed trades paginated
 * endpoint). Maker/taker user profiles, peer reputation, and price quote are kept as full VOs
 * so the client can render the catHash avatar, StarRating, and locale-formatted amounts/prices
 * without follow-up lookups. Display/derivable strings the server emits (e.g.
 * bitcoinSettlementMethodDisplayString) are dropped here — the domain model derives them
 * client-side so locale switches stay coherent.
 */
@Serializable
data class ClosedTradeListItemDto(
    val trade: TradeSlimDto,
    val makerUserProfile: UserProfileVO,
    val takerUserProfile: UserProfileVO,
    val mediatorUserProfile: UserProfileVO? = null,
    val priceQuote: PriceQuoteVO,
    val baseAmount: Long,
    val quoteAmount: Long,
    val bitcoinSettlementMethod: String,
    val fiatPaymentMethod: String,
    val peersReputationScore: ReputationScoreVO,
    val paymentAccountData: String? = null,
    val bitcoinPaymentData: String? = null,
    val paymentProof: String? = null,
    val tradeCompletedDate: Long? = null,
) {
    @Serializable
    data class TradeSlimDto(
        val id: String,
        val tradeRole: TradeRoleEnum,
        val tradeState: BisqEasyTradeStateEnum,
        val contract: ContractSlimDto,
    )

    @Serializable
    data class ContractSlimDto(
        val takeOfferDate: Long,
    )
}
