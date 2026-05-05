package network.bisq.mobile.domain.model.trade

import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.fiatToDecimal
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.formatters.PriceFormatter
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.has
import network.bisq.mobile.i18n.i18n

private const val MAIN_CHAIN_PAYMENT_METHOD = "MAIN_CHAIN"

data class ClosedTradeListItem(
    val tradeId: String,
    val peersUserProfile: UserProfileVO,
    val peersReputationScore: ReputationScoreVO,
    val mediatorUserProfile: UserProfileVO? = null,
    val myUserProfile: UserProfileVO,
    val priceQuote: PriceQuoteVO?,
    val fiatPaymentMethod: String,
    val bitcoinSettlementMethod: String,
    val isMaker: Boolean,
    val isBuyer: Boolean,
    val outcome: TradeOutcome,
    val takeOfferDate: Long,
    val tradeCompletedDate: Long?,
    val baseAmount: Long,
    val quoteAmount: Long,
    val paymentAccountData: String?,
    val bitcoinPaymentData: String?,
    val paymentProof: String?,
) {
    val myUserName: String get() = myUserProfile.userName
    val myUserNym: String get() = myUserProfile.nym

    val shortTradeId: String get() = tradeId.take(8)

    val quoteCurrencyCode: String get() = priceQuote?.market?.quoteCurrencyCode.orEmpty()

    val isOnChainSettlement: Boolean get() = bitcoinSettlementMethod == MAIN_CHAIN_PAYMENT_METHOD

    val formattedDateTime: String get() = DateUtils.toMediumDateTime(takeOfferDate)

    val directionalTitle: String
        get() =
            if (isBuyer) {
                "bisqEasy.openTrades.table.direction.buyer".i18n()
            } else {
                "bisqEasy.openTrades.table.direction.seller".i18n()
            }

    val myRole: String
        get() =
            if (isMaker) {
                "bisqEasy.openTrades.table.makerTakerRole.maker".i18n()
            } else {
                "bisqEasy.openTrades.table.makerTakerRole.taker".i18n()
            }

    val myRoleWithDirection: String
        get() =
            when {
                isBuyer && !isMaker -> "mobile.tradeHistory.role.buyerAsTaker".i18n()
                isBuyer && isMaker -> "mobile.tradeHistory.role.buyerAsMaker".i18n()
                !isBuyer && !isMaker -> "mobile.tradeHistory.role.sellerAsTaker".i18n()
                else -> "mobile.tradeHistory.role.sellerAsMaker".i18n()
            }

    val fiatPaymentMethodDisplay: String
        get() = if (has(fiatPaymentMethod)) fiatPaymentMethod.i18n() else fiatPaymentMethod

    val bitcoinSettlementMethodDisplay: String
        get() = if (has(bitcoinSettlementMethod)) bitcoinSettlementMethod.i18n() else bitcoinSettlementMethod

    /** Locale-formatted quote amount without currency code, e.g. "8,975.07". */
    val formattedQuoteAmount: String
        get() = NumberFormatter.format(quoteAmount.fiatToDecimal())

    /** Locale-formatted quote amount with currency code, e.g. "8,975.07 ARS". */
    val formattedQuoteAmountWithCode: String
        get() = "$formattedQuoteAmount $quoteCurrencyCode"

    /** BTC base amount with 8 decimal places (satoshi precision), e.g. "0.00007582". */
    val formattedBaseAmount: String
        get() = NumberFormatter.btcFormat(baseAmount)

    /** Price value, locale-formatted, e.g. "118,332,893.70". Empty when priceQuote is null. */
    val formattedPriceValue: String
        get() = priceQuote?.let { PriceFormatter.format(it) }.orEmpty()

    /** Market codes for the price row, e.g. "BTC/ARS". Empty when priceQuote is null. */
    val priceMarketCodes: String
        get() = priceQuote?.market?.marketCodes.orEmpty()

    /** Price value with market codes, e.g. "118,332,893.70 BTC/ARS". Empty when priceQuote is null. */
    val formattedPriceWithCode: String
        get() = priceQuote?.let { PriceFormatter.formatWithCode(it) }.orEmpty()
}
