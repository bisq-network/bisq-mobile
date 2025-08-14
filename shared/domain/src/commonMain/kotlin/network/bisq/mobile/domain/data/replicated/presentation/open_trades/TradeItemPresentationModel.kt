package network.bisq.mobile.domain.data.replicated.presentation.open_trades

import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.formatters.PriceSpecFormatter

/**
 * This model is used in the UI and will get the mutual fields updated from domain services.
 */
data class TradeItemPresentationModel(
    private val tradeItemPresentationDto: TradeItemPresentationDto,
    val bisqEasyOpenTradeChannelModel: BisqEasyOpenTradeChannelModel,
    val bisqEasyTradeModel: BisqEasyTradeModel,
) {

    // Delegates of tradeItemPresentationVO
    val makerUserProfile: UserProfileVO get() = tradeItemPresentationDto.makerUserProfile
    val takerUserProfile: UserProfileVO get() = tradeItemPresentationDto.takerUserProfile
    val directionalTitle: String get() = tradeItemPresentationDto.directionalTitle
    val formattedDate: String get() = tradeItemPresentationDto.formattedDate
    val formattedTime: String get() = tradeItemPresentationDto.formattedTime
    val market: String get() = tradeItemPresentationDto.market
    val price: Long get() = tradeItemPresentationDto.price
    val formattedPrice: String get() = PriceSpecFormatter.getFormattedPriceSpec(bisqEasyOffer.priceSpec, true)
    val baseAmount: Long get() = tradeItemPresentationDto.baseAmount
    val formattedBaseAmount: String get() = NumberFormatter.btcFormat(baseAmount)
    val quoteAmount: Long get() = tradeItemPresentationDto.quoteAmount
    val formattedQuoteAmount: String get() = NumberFormatter.format(quoteAmount.toDouble() / 10000.0)
    val bitcoinSettlementMethod: String get() = tradeItemPresentationDto.bitcoinSettlementMethod
    val bitcoinSettlementMethodDisplayString: String get() = tradeItemPresentationDto.bitcoinSettlementMethodDisplayString
    val fiatPaymentMethod: String get() = tradeItemPresentationDto.fiatPaymentMethod
    val fiatPaymentMethodDisplayString: String get() = tradeItemPresentationDto.fiatPaymentMethodDisplayString
    val isFiatPaymentMethodCustom: Boolean get() = tradeItemPresentationDto.isFiatPaymentMethodCustom
    val formattedMyRole: String get() = tradeItemPresentationDto.formattedMyRole

    // Convenience properties
    val myUserProfile: UserProfileVO get() = if (bisqEasyTradeModel.isMaker) tradeItemPresentationDto.makerUserProfile
                                else tradeItemPresentationDto.takerUserProfile
    val myUserName: String get() = myUserProfile.userName

    val peersUserProfile: UserProfileVO get() = if (bisqEasyTradeModel.isMaker) takerUserProfile else makerUserProfile
    val peersReputationScore: ReputationScoreVO get() = tradeItemPresentationDto.peersReputationScore
    val peersUserName: String get() = peersUserProfile.userName
    val mediator: UserProfileVO? get() = bisqEasyTradeModel.contract.mediator
    val mediatorUserName: String? get() = mediator?.userName

    val bisqEasyOffer: BisqEasyOfferVO get() = bisqEasyOpenTradeChannelModel.bisqEasyOffer
    val offerId: String get() = bisqEasyOffer.id
    val tradeId: String get() = bisqEasyTradeModel.id
    val shortTradeId: String get() = bisqEasyTradeModel.shortId
    val baseCurrencyCode: String get() = bisqEasyOffer.market.baseCurrencyCode
    val quoteCurrencyCode: String get() = bisqEasyOffer.market.quoteCurrencyCode
    val quoteAmountWithCode: String get() = "${NumberFormatter.format(quoteAmount.toDouble() / 10000.0)} $quoteCurrencyCode"
    val baseAmountWithCode: String get() = "$formattedBaseAmount $baseCurrencyCode"

    override fun toString(): String {
        return """
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
    }

    companion object {
        fun from(tradeItemPresentationDto: TradeItemPresentationDto): TradeItemPresentationModel {
            return TradeItemPresentationModel(
                tradeItemPresentationDto = tradeItemPresentationDto,
                bisqEasyOpenTradeChannelModel = BisqEasyOpenTradeChannelModel(
                    tradeItemPresentationDto.channel
                ),
                bisqEasyTradeModel = BisqEasyTradeModel(tradeItemPresentationDto.trade),
            )
        }
    }
}
