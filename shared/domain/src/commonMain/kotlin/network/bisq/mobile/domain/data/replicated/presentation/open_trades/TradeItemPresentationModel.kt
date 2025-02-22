package network.bisq.mobile.domain.data.replicated.presentation.open_trades

import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeModel

/**
 * This model is used in the UI and will get the mutual fields updated from domain services.
 */
class TradeItemPresentationModel(tradeItemPresentationDto: TradeItemPresentationDto) {
    private val bisqEasyTradeDto: BisqEasyTradeDto = tradeItemPresentationDto.trade
    val bisqEasyOpenTradeChannelModel = BisqEasyOpenTradeChannelModel(tradeItemPresentationDto.channel)
    val bisqEasyTradeModel = BisqEasyTradeModel(bisqEasyTradeDto)

    // Delegates of tradeItemPresentationVO
    val makerUserProfile = tradeItemPresentationDto.makerUserProfile
    val takerUserProfile = tradeItemPresentationDto.takerUserProfile
    val directionalTitle = tradeItemPresentationDto.directionalTitle
    val formattedDate = tradeItemPresentationDto.formattedDate
    val formattedTime = tradeItemPresentationDto.formattedTime
    val market = tradeItemPresentationDto.market
    val price = tradeItemPresentationDto.price
    val formattedPrice = tradeItemPresentationDto.formattedPrice
    val baseAmount = tradeItemPresentationDto.baseAmount
    val formattedBaseAmount = tradeItemPresentationDto.formattedBaseAmount
    val quoteAmount = tradeItemPresentationDto.quoteAmount
    val formattedQuoteAmount = tradeItemPresentationDto.formattedQuoteAmount
    val bitcoinSettlementMethod = tradeItemPresentationDto.bitcoinSettlementMethod
    val bitcoinSettlementMethodDisplayString = tradeItemPresentationDto.bitcoinSettlementMethodDisplayString
    val fiatPaymentMethod = tradeItemPresentationDto.fiatPaymentMethod
    val fiatPaymentMethodDisplayString = tradeItemPresentationDto.fiatPaymentMethodDisplayString
    val isFiatPaymentMethodCustom = tradeItemPresentationDto.isFiatPaymentMethodCustom
    val formattedMyRole = tradeItemPresentationDto.formattedMyRole

    // Convenience properties
    val myUserProfile = if (bisqEasyTradeModel.isMaker) makerUserProfile else takerUserProfile
    val myUserName = myUserProfile.userName
    val peersUserProfile = if (bisqEasyTradeModel.isMaker) takerUserProfile else makerUserProfile
    val peersReputationScore = tradeItemPresentationDto.peersReputationScore
    val peersUserName = peersUserProfile.userName
    val mediator = bisqEasyTradeModel.contract.mediator
    val mediatorUserName = mediator?.userName

    val bisqEasyOffer: BisqEasyOfferVO = bisqEasyOpenTradeChannelModel.bisqEasyOffer
    val offerId = bisqEasyOffer.id
    val tradeId = bisqEasyTradeModel.id
    val shortTradeId = bisqEasyTradeModel.shortId
    val baseCurrencyCode: String = bisqEasyOffer.market.baseCurrencyCode
    val quoteCurrencyCode: String = bisqEasyOffer.market.quoteCurrencyCode
    val quoteAmountWithCode = "$formattedQuoteAmount $quoteCurrencyCode"
    val baseAmountWithCode = "$formattedBaseAmount $baseCurrencyCode"

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
}
