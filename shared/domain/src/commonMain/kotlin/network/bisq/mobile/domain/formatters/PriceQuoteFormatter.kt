package network.bisq.mobile.domain.formatters

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.utils.decimalFormatter

object PriceQuoteFormatter {
    fun format(
        priceQuote: PriceQuoteVO,
        useLowPrecision: Boolean = true,
        withCode: Boolean = false,
    ): String {
        val doubleValue =
            BigDecimal
                .fromLong(priceQuote.quoteSideMonetary.value)
                .moveDecimalPoint(-priceQuote.quoteSideMonetary.precision)
                .doubleValue(false)
        val precision = if (useLowPrecision) priceQuote.quoteSideMonetary.lowPrecision else priceQuote.quoteSideMonetary.precision
        val formatted = decimalFormatter.format(doubleValue, precision)
        return if (withCode) "$formatted ${priceQuote.market.marketCodes}" else formatted
    }
}
