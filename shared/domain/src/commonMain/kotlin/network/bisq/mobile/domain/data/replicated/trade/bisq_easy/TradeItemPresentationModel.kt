package network.bisq.mobile.domain.data.replicated.trade.bisq_easy

import network.bisq.mobile.domain.data.presentation.open_trades.TradeItemPresentationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO

/**
 * This model is used in the UI and will get the mutual fields updated from domain services.
 */
class TradeItemPresentationModel(tradeItemPresentationVO: TradeItemPresentationVO) {
    private val bisqEasyTradeVO: BisqEasyTradeVO = tradeItemPresentationVO.trade
    val bisqEasyOpenTradeChannelModel = BisqEasyOpenTradeChannelModel(tradeItemPresentationVO.channel)
    val bisqEasyTradeModel = BisqEasyTradeModel(bisqEasyTradeVO)

    // Delegates of tradeItemPresentationVO
    val makerUserProfile = tradeItemPresentationVO.makerUserProfile
    val takerUserProfile = tradeItemPresentationVO.takerUserProfile
    val directionalTitle = tradeItemPresentationVO.directionalTitle
    val formattedDate = tradeItemPresentationVO.formattedDate
    val formattedTime = tradeItemPresentationVO.formattedTime
    val market = tradeItemPresentationVO.market
    val price = tradeItemPresentationVO.price
    val formattedPrice = tradeItemPresentationVO.formattedPrice
    val baseAmount = tradeItemPresentationVO.baseAmount
    val formattedBaseAmount = tradeItemPresentationVO.formattedBaseAmount
    val quoteAmount = tradeItemPresentationVO.quoteAmount
    val formattedQuoteAmount = tradeItemPresentationVO.formattedQuoteAmount
    val bitcoinSettlementMethod = tradeItemPresentationVO.bitcoinSettlementMethod
    val bitcoinSettlementMethodDisplayString = tradeItemPresentationVO.bitcoinSettlementMethodDisplayString
    val fiatPaymentMethod = tradeItemPresentationVO.fiatPaymentMethod
    val fiatPaymentMethodDisplayString = tradeItemPresentationVO.fiatPaymentMethodDisplayString
    val isFiatPaymentMethodCustom = tradeItemPresentationVO.isFiatPaymentMethodCustom
    val formattedMyRole = tradeItemPresentationVO.formattedMyRole

    // Convenience properties
    val myUserProfile = if (bisqEasyTradeModel.isMaker) makerUserProfile else takerUserProfile
    val myUserName = myUserProfile.userName
    val peersUserProfile = if (bisqEasyTradeModel.isMaker) takerUserProfile else makerUserProfile
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
}
