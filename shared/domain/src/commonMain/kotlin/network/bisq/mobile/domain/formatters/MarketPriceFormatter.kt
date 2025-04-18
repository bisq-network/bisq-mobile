package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.decimalFormatter

object MarketPriceFormatter {
    fun format(quote: Long, market: MarketVO): String {
        val doubleValue: Double = quote.toDouble() / 10000
        val stringValue: String = decimalFormatter.format(doubleValue, 2) //doubleValue.roundTo(2).toString()
        return stringValue + " " + market.marketCodes
    }
}