package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.common.currency.marketCodes
import network.bisq.mobile.domain.utils.MathUtils.roundTo

object MarketPriceFormatter {
    fun format(quote: Long, market: MarketVO): String {
        val doubleValue: Double = quote.toDouble() / 10000
        val stringValue: String = doubleValue.roundTo(2).toString()
        return stringValue + " " + market.marketCodes
    }
}