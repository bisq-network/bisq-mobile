package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.replicated.common.monetary.asDouble
import network.bisq.mobile.domain.replicated.common.monetary.fromPrice
import network.bisq.mobile.domain.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.MathUtils.roundTo


object PriceUtil {

    /**
     * A quote created from a market price quote and a percentage
     *
     * @param marketPrice Current market price
     * @param percentage  Offset from market price in percent normalize to 1 (=100%).
     * @return The quote representing the offset from market price
     */
    fun fromMarketPriceMarkup(marketPrice: PriceQuoteVO, percentage: Double): PriceQuoteVO {
        require(percentage >= -1) { "Percentage must not be lower than -100%" }
        val price = marketPrice.asDouble() * (1 + percentage)
        return PriceQuoteVO.fromPrice(price, marketPrice.market);
    }

    /**
     * @param marketPrice The quote representing the market price
     * @param priceQuote  The quote we want to compare to the market price
     * @return The percentage offset from the market price. Positive value means that quote is above market price.
     * Result is rounded to precision 4 (2 decimal places at percentage representation)
     */
    fun getPercentageToMarketPrice(marketPrice: PriceQuoteVO, priceQuote: PriceQuoteVO): Double {
        require(marketPrice.value > 0) { "marketQuote must be positive" }
        val res = priceQuote.value / marketPrice.value.toDouble() - 1
        return res.roundTo(4)

        //return MathUtils.roundDouble(priceQuote.getValue() / marketPrice.getValue() as Double - 1, 4)
    }

    fun findPercentFromMarketPrice(marketPriceService: MarketPriceServiceFacade, priceSpec: PriceSpecVO, market: MarketVO): Double {
        val percentage: Double
        if (priceSpec is FixPriceSpecVO) {
            val fixPrice: PriceQuoteVO = priceSpec.priceQuote
            val marketPriceItem = marketPriceService.findMarketPriceItem(market)
            val marketPrice = marketPriceItem!!.priceQuote
            percentage = getPercentageToMarketPrice(marketPrice, fixPrice)
        } else if (priceSpec is MarketPriceSpecVO) {
            percentage = 0.0
        } else if (priceSpec is FloatPriceSpecVO) {
            percentage = priceSpec.percentage
        } else {
            throw IllegalStateException("Not supported priceSpec. priceSpec=$priceSpec")
        }
        return percentage
    }
}