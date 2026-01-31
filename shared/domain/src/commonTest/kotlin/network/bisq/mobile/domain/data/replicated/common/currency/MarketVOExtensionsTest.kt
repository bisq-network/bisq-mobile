package network.bisq.mobile.domain.data.replicated.common.currency

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketVOExtensionsTest {
    @Test
    fun `marketCodes returns correct format for BTC-USD`() {
        val market = MarketVO("BTC", "USD")
        assertEquals("BTC/USD", market.marketCodes)
    }

    @Test
    fun `marketCodes returns correct format for BTC-EUR`() {
        val market = MarketVO("BTC", "EUR")
        assertEquals("BTC/EUR", market.marketCodes)
    }

    @Test
    fun `marketCodes returns correct format with currency names`() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        assertEquals("BTC/USD", market.marketCodes)
    }

    @Test
    fun `marketCodes handles various currency pairs`() {
        val markets =
            listOf(
                MarketVO("BTC", "ARS") to "BTC/ARS",
                MarketVO("BTC", "GBP") to "BTC/GBP",
                MarketVO("BTC", "JPY") to "BTC/JPY",
                MarketVO("BTC", "CHF") to "BTC/CHF",
            )

        markets.forEach { (market, expected) ->
            assertEquals(expected, market.marketCodes)
        }
    }
}
