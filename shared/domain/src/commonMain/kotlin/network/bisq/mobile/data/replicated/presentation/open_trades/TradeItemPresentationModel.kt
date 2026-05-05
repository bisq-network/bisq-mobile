package network.bisq.mobile.data.replicated.presentation.open_trades

import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.i18n.i18n

/**
 * This model is used in the UI and will get the mutual fields updated from domain services.
 */
data class TradeItemPresentationModel(
    private val tradeItemPresentationDto: TradeItemPresentationDto,
    private val channelModel: BisqEasyOpenTradeChannelModel?,
    val bisqEasyTradeModel: BisqEasyTradeModel,
) {
    // Non-null accessor: throws for closed trades if called in open-trade context
    val bisqEasyOpenTradeChannelModel: BisqEasyOpenTradeChannelModel
        get() = channelModel ?: error("Trade $tradeId has no channel (closed trade)")

    // Delegates of tradeItemPresentationVO
    val makerUserProfile: UserProfileVO get() = tradeItemPresentationDto.makerUserProfile
    val takerUserProfile: UserProfileVO get() = tradeItemPresentationDto.takerUserProfile

    val directionalTitle: String
        get() =
            if (bisqEasyTradeModel.isSeller) {
                "bisqEasy.openTrades.table.direction.seller".i18n().uppercase()
            } else {
                "bisqEasy.openTrades.table.direction.buyer".i18n().uppercase()
            }

    val formattedDate: String get() = tradeItemPresentationDto.formattedDate
    val formattedTime: String get() = tradeItemPresentationDto.formattedTime
    val market: String get() = tradeItemPresentationDto.market
    val price: Long get() = tradeItemPresentationDto.price
    val formattedPrice: String get() = tradeItemPresentationDto.formattedPrice
    val formattedPriceSpec: String
        get() {
            val spec = bisqEasyOffer.priceSpec
            return if (spec is FixPriceSpecVO) "" else "(${PriceSpecFormatter.getFormattedPriceSpec(spec, true)})"
        }
    val baseAmount: Long get() = tradeItemPresentationDto.baseAmount
    val formattedBaseAmount: String get() = tradeItemPresentationDto.formattedBaseAmount
    val quoteAmount: Long get() = tradeItemPresentationDto.quoteAmount
    val formattedQuoteAmount: String get() = tradeItemPresentationDto.formattedQuoteAmount
    val bitcoinSettlementMethod: String get() = tradeItemPresentationDto.bitcoinSettlementMethod
    val bitcoinSettlementMethodDisplayString: String get() = tradeItemPresentationDto.bitcoinSettlementMethodDisplayString
    val fiatPaymentMethod: String get() = tradeItemPresentationDto.fiatPaymentMethod
    val fiatPaymentMethodDisplayString: String get() = tradeItemPresentationDto.fiatPaymentMethodDisplayString
    val paymentMethodCsvDisplayString: String
        get() = "$bitcoinSettlementMethodDisplayString / $fiatPaymentMethodDisplayString"
    val isFiatPaymentMethodCustom: Boolean get() = tradeItemPresentationDto.isFiatPaymentMethodCustom
    val formattedMyRole: String get() = tradeItemPresentationDto.formattedMyRole

    // Convenience properties
    val myUserProfile: UserProfileVO
        get() =
            if (bisqEasyTradeModel.isMaker) {
                tradeItemPresentationDto.makerUserProfile
            } else {
                tradeItemPresentationDto.takerUserProfile
            }
    val myUserName: String get() = myUserProfile.userName

    val peersUserProfile: UserProfileVO get() = if (bisqEasyTradeModel.isMaker) takerUserProfile else makerUserProfile
    val peersReputationScore: ReputationScoreVO get() = tradeItemPresentationDto.peersReputationScore
    val peersUserName: String get() = peersUserProfile.userName
    val mediator: UserProfileVO? get() = bisqEasyTradeModel.contract.mediator
    val mediatorUserName: String? get() = mediator?.userName

    val bisqEasyOffer: BisqEasyOfferVO
        get() = channelModel?.bisqEasyOffer ?: bisqEasyTradeModel.contract.offer
    val offerId: String get() = bisqEasyOffer.id
    val tradeId: String get() = bisqEasyTradeModel.id
    val shortTradeId: String get() = bisqEasyTradeModel.shortId
    val baseCurrencyCode: String get() = bisqEasyOffer.market.baseCurrencyCode
    val quoteCurrencyCode: String get() = bisqEasyOffer.market.quoteCurrencyCode
    val quoteAmountWithCode: String get() = "$formattedQuoteAmount $quoteCurrencyCode"
    val baseAmountWithCode: String get() = "$formattedBaseAmount $baseCurrencyCode"

    override fun toString(): String =
        """
        TradeItemPresentationModel(
            tradeId=$tradeId,
            shortTradeId=$shortTradeId,
            offerId=$offerId,
            baseCurrencyCode=$baseCurrencyCode,
            quoteCurrencyCode=$quoteCurrencyCode,
            quoteAmountWithCode=$quoteAmountWithCode,
            baseAmountWithCode=$baseAmountWithCode,
            makerUserName=${makerUserProfile.userName},
            takerUserName=${takerUserProfile.userName},
            myUserName=$myUserName,
            peersUserName=$peersUserName,
            formattedDate=$formattedDate,
            formattedTime=$formattedTime,
            market=$market,
            price=$price,
            formattedPrice=$formattedPrice,
            bitcoinSettlementMethod=$bitcoinSettlementMethodDisplayString,
            fiatPaymentMethod=$fiatPaymentMethodDisplayString,
            mediatorUserName=$mediatorUserName
        )
        """.trimIndent()

    companion object {
        fun from(tradeItemPresentationDto: TradeItemPresentationDto): TradeItemPresentationModel =
            TradeItemPresentationModel(
                tradeItemPresentationDto = tradeItemPresentationDto,
                channelModel = BisqEasyOpenTradeChannelModel(tradeItemPresentationDto.channel),
                bisqEasyTradeModel = BisqEasyTradeModel(tradeItemPresentationDto.trade),
            )
    }
}
