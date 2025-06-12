package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.asDouble
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
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
        return PriceQuoteVOFactory.fromPrice(price, marketPrice.market);
    }

    /**
     * @param marketPrice The quote representing the market price
     * @param priceQuote  The quote we want to compare to the market price
     * @return The percentage offset from the market price. Positive value means that quote is above market price.
     * Result is rounded to precision 4 (2 decimal places at percentage representation)
     */
    fun getPercentageToMarketPrice(marketPrice: PriceQuoteVO, priceQuote: PriceQuoteVO): Double {
        require(marketPrice.value > 0) { "marketQuote must be positive" }
        require(priceQuote.value >= 0) { "priceQuote must be non-negative" }

        // Additional safety check for iOS compatibility
        val marketPriceDouble = marketPrice.value.toDouble()
        if (marketPriceDouble == 0.0 || !marketPriceDouble.isFinite()) {
            throw IllegalArgumentException("Invalid market price value: $marketPriceDouble")
        }

        val priceQuoteDouble = priceQuote.value.toDouble()
        if (!priceQuoteDouble.isFinite()) {
            throw IllegalArgumentException("Invalid price quote value: $priceQuoteDouble")
        }

        val res = priceQuoteDouble / marketPriceDouble - 1

        // Ensure result is finite before rounding
        if (!res.isFinite()) {
            throw IllegalArgumentException("Calculation resulted in non-finite value: $res")
        }

        return res.roundTo(4)
    }

    fun findPercentFromMarketPrice(
        marketPriceService: MarketPriceServiceFacade,
        priceSpec: PriceSpecVO,
        market: MarketVO
    ): Double {
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