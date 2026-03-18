package network.bisq.mobile.domain.formatters

import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.asDouble
import network.bisq.mobile.data.utils.decimalFormatter

object PriceFormatter {
    fun formatWithCode(
        priceQuote: PriceQuoteVO,
        useLowPrecision: Boolean = true,
    ): String {
        val formatted = format(priceQuote, useLowPrecision)
        return "$formatted ${priceQuote.market.marketCodes}"
    }

    fun format(
        priceQuote: PriceQuoteVO,
        useLowPrecision: Boolean = true,
    ): String {
        val precision = if (useLowPrecision) priceQuote.lowPrecision else priceQuote.precision
        return decimalFormatter.format(priceQuote.asDouble(), precision)
    }
}
