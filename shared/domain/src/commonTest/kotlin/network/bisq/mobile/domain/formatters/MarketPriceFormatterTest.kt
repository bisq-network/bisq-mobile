package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import kotlin.test.Test
import kotlin.test.assertTrue

class MarketPriceFormatterTest {
    private val btcUsdMarket = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
    private val btcEurMarket = MarketVO("BTC", "EUR", "Bitcoin", "Euro")

    @Test
    fun `format includes market codes`() {
        val result = MarketPriceFormatter.format(100_0000L, btcUsdMarket)
        assertTrue(result.contains("BTC/USD"))
    }

    @Test
    fun `format handles zero quote`() {
        val result = MarketPriceFormatter.format(0L, btcUsdMarket)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `format handles different markets`() {
        val result = MarketPriceFormatter.format(100_0000L, btcEurMarket)
        assertTrue(result.contains("BTC/EUR"))
    }

    @Test
    fun `format converts quote correctly`() {
        // 100_0000L / 10000 = 100.0
        val result = MarketPriceFormatter.format(100_0000L, btcUsdMarket)
        assertTrue(result.contains("100"))
    }

    @Test
    fun `format handles fractional values`() {
        // 12345L / 10000 = 1.2345, rounded to 2 decimals = 1.23
        val result = MarketPriceFormatter.format(12345L, btcUsdMarket)
        assertTrue(result.contains("1.23") || result.contains("1,23"))
    }
}
