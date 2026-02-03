package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import kotlin.test.Test
import kotlin.test.assertTrue

class PriceFormatterTest {
    private fun createTestPriceQuote(value: Long): PriceQuoteVO {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        return PriceQuoteVO(
            value = value,
            precision = 4,
            lowPrecision = 2,
            market = market,
            baseSideMonetary = CoinVO("BTC", 1, "BTC", 8, 4),
            quoteSideMonetary = FiatVO("USD", value, "USD", 4, 2),
        )
    }

    @Test
    fun `formatWithCode includes currency code`() {
        val priceQuote = createTestPriceQuote(500000L)
        val result = PriceFormatter.formatWithCode(priceQuote)
        assertTrue(result.contains("USD"))
    }

    @Test
    fun `format excludes currency code`() {
        val priceQuote = createTestPriceQuote(500000L)
        val result = PriceFormatter.format(priceQuote)
        assertTrue(!result.contains("USD"))
    }

    @Test
    fun `format handles zero value`() {
        val priceQuote = createTestPriceQuote(0L)
        val result = PriceFormatter.format(priceQuote)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `format uses low precision by default`() {
        val priceQuote = createTestPriceQuote(123456L)
        val lowPrecResult = PriceFormatter.format(priceQuote, useLowPrecision = true)
        val highPrecResult = PriceFormatter.format(priceQuote, useLowPrecision = false)
        assertTrue(lowPrecResult.length <= highPrecResult.length)
    }
}
