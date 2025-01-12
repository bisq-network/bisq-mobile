package network.bisq.mobile.domain.data.replicated.offer.price.spec

import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import kotlin.math.roundToLong

object PriceSpecVOExtensions {
    fun PriceSpecVO.getPriceQuoteVO(marketPriceItem: MarketPriceItem): PriceQuoteVO {
        if (this is FixPriceSpecVO) {
            return this.priceQuote
        } else if (this is FloatPriceSpecVO) {
            val floatPricePercentage: Double = this.percentage
            val adjustedPrice = marketPriceItem.priceQuote.value * (1 + floatPricePercentage)
            val priceValue = adjustedPrice.roundToLong()
            return PriceQuoteVOFactory.fromPrice(
                priceValue,
                marketPriceItem.market.baseCurrencyCode,
                marketPriceItem.market.quoteCurrencyCode
            )
        } else {
            // MarketPriceSpec
            return marketPriceItem.priceQuote
        }
    }
}