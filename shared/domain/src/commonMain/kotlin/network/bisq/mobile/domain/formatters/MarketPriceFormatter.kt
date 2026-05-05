package network.bisq.mobile.domain.formatters

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.common.monetary.fiatToDecimal
import network.bisq.mobile.data.utils.decimalFormatter

object MarketPriceFormatter {
    fun format(
        quote: Long,
        market: MarketVO,
    ): String {
        val stringValue: String = decimalFormatter.format(quote.fiatToDecimal(), 2)
        return stringValue + " " + market.marketCodes
    }
}
