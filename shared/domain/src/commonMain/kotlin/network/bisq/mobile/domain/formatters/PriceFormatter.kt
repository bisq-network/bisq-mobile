package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toDouble
import network.bisq.mobile.domain.decimalFormatter

object PriceFormatter {
    fun formatWithCode(priceQuote: PriceQuoteVO, useLowPrecision: Boolean = true): String {
        val formatted = format(priceQuote, useLowPrecision)
        return "$formatted ${priceQuote.market.quoteCurrencyCode}"
    }

    fun format(priceQuote: PriceQuoteVO, useLowPrecision: Boolean = true): String {
        val precision = if (useLowPrecision) priceQuote.lowPrecision else priceQuote.precision
        return decimalFormatter.format(priceQuote.toDouble(2), precision)
    }
}
