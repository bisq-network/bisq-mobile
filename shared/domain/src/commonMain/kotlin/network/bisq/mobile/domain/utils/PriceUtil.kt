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
import kotlin.math.pow



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
     * Result is rounded to precision 4 (2 decimal places at percentage representation).
     *
     * Auto-correction note:
     * If the raw ratio priceQuote/marketPrice falls outside [0.1, 10], we heuristically
     * attempt a 10^n scale correction (n in [-4, +4]) only when BOTH quotes are for the
     * same market (same base and quote codes). This protects against accidental extra scaling
     * of user input while avoiding cross-market interference.
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

        // Compute raw ratio first
        var ratio = priceQuoteDouble / marketPriceDouble

        // If ratio is far from 1, try to auto-correct potential 10^n scale mismatches.
        // We only attempt this when both quotes refer to the same market and currencies.
        if (ratio > 10.0 || ratio < 0.1) {
            val sameMarket = marketPrice.market.baseCurrencyCode == priceQuote.market.baseCurrencyCode &&
                    marketPrice.market.quoteCurrencyCode == priceQuote.market.quoteCurrencyCode
            if (sameMarket) {
                val downScales = (1..4).map { 1 / 10.0.pow(it) }.toDoubleArray()
                val upScales = (1..4).map { 10.0.pow(it) }.toDoubleArray()
                val candidates = doubleArrayOf(1.0) + downScales + upScales

                var bestScore = kotlin.math.abs(ratio - 1.0)
                for (scale in candidates) {
                    val r = (priceQuoteDouble * scale) / marketPriceDouble
                    val score = kotlin.math.abs(r - 1.0)
                    if (score < bestScore) {
                        bestScore = score
                        ratio = r
                    }
                }
                // ratio updated to the best candidate
            }
        }

        val res = ratio - 1

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
            val marketPrice = marketPriceItem?.priceQuote
            // for demo mode
            percentage = if (marketPrice == null) 0.0 else getPercentageToMarketPrice(marketPrice, fixPrice)
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