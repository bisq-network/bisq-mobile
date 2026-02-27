package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PriceSpecFormatterTest {
    @BeforeTest
    fun setup() {
        I18nSupport.initialize("en")
    }

    private fun createTestMarket(): MarketVO =
        MarketVO(
            baseCurrencyCode = "BTC",
            quoteCurrencyCode = "USD",
            baseCurrencyName = "Bitcoin",
            quoteCurrencyName = "US Dollar",
        )

    private fun createTestPriceQuote(value: Long): PriceQuoteVO {
        val market = createTestMarket()
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
    fun `getFormattedPriceSpec formats FixPriceSpec correctly`() {
        val priceQuote = createTestPriceQuote(500000000)
        val priceSpec = FixPriceSpecVO(priceQuote)
        val result = PriceSpecFormatter.getFormattedPriceSpec(priceSpec)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getFormattedPriceSpec formats FloatPriceSpec with positive percentage`() {
        val priceSpec = FloatPriceSpecVO(0.05) // 5% above
        val result = PriceSpecFormatter.getFormattedPriceSpec(priceSpec)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("5"))
    }

    @Test
    fun `getFormattedPriceSpec formats FloatPriceSpec with negative percentage`() {
        val priceSpec = FloatPriceSpecVO(-0.05) // 5% below
        val result = PriceSpecFormatter.getFormattedPriceSpec(priceSpec)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("5"))
    }

    @Test
    fun `getFormattedPriceSpec formats FloatPriceSpec abbreviated`() {
        val priceSpec = FloatPriceSpecVO(0.10) // 10% above
        val result = PriceSpecFormatter.getFormattedPriceSpec(priceSpec, abbreviated = true)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getFormattedPriceSpec formats MarketPriceSpec`() {
        val priceSpec = MarketPriceSpecVO()
        val result = PriceSpecFormatter.getFormattedPriceSpec(priceSpec)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getFormattedPriceSpecWithOfferPrice formats FixPriceSpec`() {
        val priceQuote = createTestPriceQuote(500000000)
        val priceSpec = FixPriceSpecVO(priceQuote)
        val result = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(priceSpec, "50,000 USD")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getFormattedPriceSpecWithOfferPrice formats FloatPriceSpec above`() {
        val priceSpec = FloatPriceSpecVO(0.05)
        val result = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(priceSpec, "50,000 USD")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getFormattedPriceSpecWithOfferPrice formats FloatPriceSpec below`() {
        val priceSpec = FloatPriceSpecVO(-0.05)
        val result = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(priceSpec, "50,000 USD")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getFormattedPriceSpecWithOfferPrice formats MarketPriceSpec`() {
        val priceSpec = MarketPriceSpecVO()
        val result = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(priceSpec, "50,000 USD")
        assertTrue(result.isNotEmpty())
    }

    // --- formatPriceWithSpec tests ---

    @Test
    fun `formatPriceWithSpec with FixPriceSpec returns just the price`() {
        val priceQuote = createTestPriceQuote(500000000)
        val priceSpec = FixPriceSpecVO(priceQuote)
        val result = PriceSpecFormatter.formatPriceWithSpec("50,000 USD", priceSpec)
        assertEquals("50,000 USD", result)
    }

    @Test
    fun `formatPriceWithSpec with FloatPriceSpec positive appends abbreviated spec`() {
        val priceSpec = FloatPriceSpecVO(0.05) // 5% above
        val result = PriceSpecFormatter.formatPriceWithSpec("105,000 USD", priceSpec)
        // Should be "105,000 USD (+5%)" or similar with abbreviated spec in parentheses
        assertTrue(result.startsWith("105,000 USD ("), "Result should start with price and open paren, was: $result")
        assertTrue(result.endsWith(")"), "Result should end with close paren, was: $result")
        assertTrue(result.contains("5"), "Result should contain the percentage value, was: $result")
    }

    @Test
    fun `formatPriceWithSpec with FloatPriceSpec negative appends abbreviated spec`() {
        val priceSpec = FloatPriceSpecVO(-0.03) // 3% below
        val result = PriceSpecFormatter.formatPriceWithSpec("97,000 USD", priceSpec)
        assertTrue(result.startsWith("97,000 USD ("), "Result should start with price and open paren, was: $result")
        assertTrue(result.endsWith(")"), "Result should end with close paren, was: $result")
        assertTrue(result.contains("3"), "Result should contain the percentage value, was: $result")
    }

    @Test
    fun `formatPriceWithSpec with MarketPriceSpec appends market price label`() {
        val priceSpec = MarketPriceSpecVO()
        val result = PriceSpecFormatter.formatPriceWithSpec("100,000 USD", priceSpec)
        assertTrue(result.startsWith("100,000 USD ("), "Result should start with price and open paren, was: $result")
        assertTrue(result.endsWith(")"), "Result should end with close paren, was: $result")
    }
}
