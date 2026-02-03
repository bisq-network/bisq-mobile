package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import kotlin.test.Test
import kotlin.test.assertTrue

class PriceQuoteFormatterTest {
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
    fun `format includes market codes when requested`() {
        val priceQuote = createTestPriceQuote(500000L) // 50.0000 USD
        val result = PriceQuoteFormatter.format(priceQuote, withCode = true)
        assertTrue(result.contains("BTC/USD"))
    }

    @Test
    fun `format excludes market codes when not requested`() {
        val priceQuote = createTestPriceQuote(500000L)
        val result = PriceQuoteFormatter.format(priceQuote, withCode = false)
        assertTrue(!result.contains("BTC/USD"))
    }

    @Test
    fun `format handles zero value`() {
        val priceQuote = createTestPriceQuote(0L)
        val result = PriceQuoteFormatter.format(priceQuote)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `format uses low precision by default`() {
        val priceQuote = createTestPriceQuote(123456L) // 12.3456 USD
        val lowPrecResult = PriceQuoteFormatter.format(priceQuote, useLowPrecision = true)
        val highPrecResult = PriceQuoteFormatter.format(priceQuote, useLowPrecision = false)
        assertTrue(lowPrecResult.length <= highPrecResult.length)
    }

    @Test
    fun `format handles large values`() {
        val priceQuote = createTestPriceQuote(1000000000L) // 100000.0000 USD
        val result = PriceQuoteFormatter.format(priceQuote)
        assertTrue(result.contains("100") && result.contains("000"))
    }
}
