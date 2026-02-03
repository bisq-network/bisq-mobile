package network.bisq.mobile.domain.data.replicated.common.monetary

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PriceQuoteVOFactoryTest {
    @Test
    fun `fromPrice with Double and MarketVO creates valid PriceQuoteVO`() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val priceQuote = PriceQuoteVOFactory.fromPrice(50000.0, market)
        assertEquals("BTC", priceQuote.market.baseCurrencyCode)
        assertEquals("USD", priceQuote.market.quoteCurrencyCode)
    }

    @Test
    fun `fromPrice with Long and MarketVO creates valid PriceQuoteVO`() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val priceQuote = PriceQuoteVOFactory.fromPrice(500000000L, market)
        assertEquals("BTC", priceQuote.market.baseCurrencyCode)
        assertEquals("USD", priceQuote.market.quoteCurrencyCode)
    }

    @Test
    fun `fromPrice with Double and currency codes creates valid PriceQuoteVO`() {
        val priceQuote = PriceQuoteVOFactory.fromPrice(50000.0, "BTC", "USD")
        assertEquals("BTC", priceQuote.market.baseCurrencyCode)
        assertEquals("USD", priceQuote.market.quoteCurrencyCode)
    }

    @Test
    fun `fromPrice with Long and currency codes creates valid PriceQuoteVO`() {
        val priceQuote = PriceQuoteVOFactory.fromPrice(500000000L, "BTC", "USD")
        assertEquals("BTC", priceQuote.market.baseCurrencyCode)
        assertEquals("USD", priceQuote.market.quoteCurrencyCode)
    }

    @Test
    fun `fromPrice handles EUR quote currency`() {
        val priceQuote = PriceQuoteVOFactory.fromPrice(45000.0, "BTC", "EUR")
        assertEquals("EUR", priceQuote.market.quoteCurrencyCode)
    }

    @Test
    fun `from with MonetaryVO creates valid PriceQuoteVO`() {
        val baseSide = CoinVO("BTC", 100000000, "BTC", 8, 4)
        val quoteSide = FiatVO("USD", 500000000, "USD", 4, 2)
        val priceQuote = PriceQuoteVOFactory.from(baseSide, quoteSide)
        assertTrue(priceQuote.value > 0)
    }

    @Test
    fun `from throws for zero baseSideMonetary value`() {
        val baseSide = CoinVO("BTC", 0, "BTC", 8, 4)
        val quoteSide = FiatVO("USD", 500000000, "USD", 4, 2)
        assertFailsWith<IllegalArgumentException> {
            PriceQuoteVOFactory.from(baseSide, quoteSide)
        }
    }

    @Test
    fun `fromPrice handles fiat base currency`() {
        // Edge case: fiat as base currency
        val priceQuote = PriceQuoteVOFactory.fromPrice(0.00002, "USD", "BTC")
        assertEquals("USD", priceQuote.market.baseCurrencyCode)
        assertEquals("BTC", priceQuote.market.quoteCurrencyCode)
    }
}
