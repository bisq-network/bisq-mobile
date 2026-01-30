package network.bisq.mobile.domain.data.replicated.common.currency

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketVOTest {
    @Test
    fun `properties are accessible`() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        assertEquals("BTC", market.baseCurrencyCode)
        assertEquals("USD", market.quoteCurrencyCode)
        assertEquals("Bitcoin", market.baseCurrencyName)
        assertEquals("US Dollar", market.quoteCurrencyName)
    }

    @Test
    fun `default names equal codes`() {
        val market = MarketVO("BTC", "EUR")
        assertEquals("BTC", market.baseCurrencyName)
        assertEquals("EUR", market.quoteCurrencyName)
    }

    @Test
    fun `data class equality works`() {
        val market1 = MarketVO("BTC", "USD")
        val market2 = MarketVO("BTC", "USD")
        assertEquals(market1, market2)
    }

    @Test
    fun `data class copy works`() {
        val original = MarketVO("BTC", "USD")
        val copied = original.copy(quoteCurrencyCode = "EUR")
        assertEquals("BTC", copied.baseCurrencyCode)
        assertEquals("EUR", copied.quoteCurrencyCode)
    }

    @Test
    fun `marketListDemoObj contains expected markets`() {
        assertTrue(marketListDemoObj.isNotEmpty())
        assertEquals(9, marketListDemoObj.size)

        val usdMarket = marketListDemoObj.find { it.quoteCurrencyCode == "USD" }
        assertEquals("BTC", usdMarket?.baseCurrencyCode)
    }

    @Test
    fun `marketListDemoObj all have BTC as base`() {
        assertTrue(marketListDemoObj.all { it.baseCurrencyCode == "BTC" })
    }

    @Test
    fun `marketListDemoObj contains specific currencies`() {
        val quoteCurrencies = marketListDemoObj.map { it.quoteCurrencyCode }
        assertTrue(quoteCurrencies.contains("USD"))
        assertTrue(quoteCurrencies.contains("EUR"))
        assertTrue(quoteCurrencies.contains("ARS"))
        assertTrue(quoteCurrencies.contains("PYG"))
        assertTrue(quoteCurrencies.contains("LBP"))
        assertTrue(quoteCurrencies.contains("CZK"))
        assertTrue(quoteCurrencies.contains("AUD"))
        assertTrue(quoteCurrencies.contains("CAD"))
        assertTrue(quoteCurrencies.contains("IDR"))
    }

    @Test
    fun `hashCode is consistent`() {
        val market1 = MarketVO("BTC", "USD")
        val market2 = MarketVO("BTC", "USD")
        assertEquals(market1.hashCode(), market2.hashCode())
    }

    @Test
    fun `marketCodes extension returns correct format`() {
        val market = MarketVO("BTC", "USD")
        assertEquals("BTC/USD", market.marketCodes)
    }

    @Test
    fun `marketCodes extension works for different currencies`() {
        val market = MarketVO("BTC", "EUR")
        assertEquals("BTC/EUR", market.marketCodes)
    }

    @Test
    fun `MarketVOFactory EMPTY has empty codes`() {
        assertEquals("", MarketVOFactory.EMPTY.baseCurrencyCode)
        assertEquals("", MarketVOFactory.EMPTY.quoteCurrencyCode)
    }

    @Test
    fun `MarketVOFactory USD has correct values`() {
        assertEquals("BTC", MarketVOFactory.USD.baseCurrencyCode)
        assertEquals("USD", MarketVOFactory.USD.quoteCurrencyCode)
        assertEquals("Bitcoin", MarketVOFactory.USD.baseCurrencyName)
        assertEquals("US Dollar", MarketVOFactory.USD.quoteCurrencyName)
    }
}
