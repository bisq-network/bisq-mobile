package network.bisq.mobile.domain.formatters

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import network.bisq.mobile.domain.decimalFormatter
import network.bisq.mobile.domain.replicated.common.currency.marketCodes
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.replicated.common.monetary.quoteSideMonetary

object PriceQuoteFormatter {
    fun format(priceQuote: PriceQuoteVO, useLowPrecision: Boolean = true, withCode: Boolean = false): String {
        val doubleValue = BigDecimal.fromLong(priceQuote.quoteSideMonetary.value)
            .moveDecimalPoint(-priceQuote.quoteSideMonetary.precision)
            .doubleValue(false)
        val precision = if (useLowPrecision) priceQuote.quoteSideMonetary.lowPrecision else priceQuote.quoteSideMonetary.precision
        val formatted = decimalFormatter.format(doubleValue, precision)
        return if (withCode) "$formatted ${priceQuote.market.marketCodes}" else formatted
    }
}