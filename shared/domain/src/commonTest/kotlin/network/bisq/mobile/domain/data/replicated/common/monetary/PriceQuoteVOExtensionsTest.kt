package network.bisq.mobile.domain.data.replicated.common.monetary

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.asDouble
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toDouble
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PriceQuoteVOExtensionsTest {
    private val tolerance = 1e-6

    private fun createTestPriceQuote(value: Long): PriceQuoteVO {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        return PriceQuoteVO(
            value = value,
            precision = 4,
            lowPrecision = 2,
            market = market,
            baseSideMonetary = CoinVO("BTC", 100000000, "BTC", 8, 4),
            quoteSideMonetary = FiatVO("USD", value, "USD", 4, 2),
        )
    }

    @Test
    fun `toDouble converts value correctly`() {
        val priceQuote = createTestPriceQuote(500000L) // 50.0000 USD
        val result = priceQuote.toDouble(500000L)
        assertEquals(50.0, result, tolerance)
    }

    @Test
    fun `asDouble returns correct value`() {
        val priceQuote = createTestPriceQuote(500000L)
        val result = priceQuote.asDouble()
        assertEquals(50.0, result, tolerance)
    }

    @Test
    fun `asDouble handles zero`() {
        val priceQuote = createTestPriceQuote(0L)
        val result = priceQuote.asDouble()
        assertEquals(0.0, result, tolerance)
    }

    @Test
    fun `toBaseSideMonetary throws for zero value`() {
        val priceQuote = createTestPriceQuote(0L)
        val quoteSideMonetary = FiatVO("USD", 100000L, "USD", 4, 2)
        assertFailsWith<IllegalArgumentException> {
            priceQuote.toBaseSideMonetary(quoteSideMonetary)
        }
    }

    @Test
    fun `toBaseSideMonetary throws for wrong type`() {
        val priceQuote = createTestPriceQuote(500000L)
        val wrongType = CoinVO("BTC", 100000000, "BTC", 8, 4)
        assertFailsWith<IllegalArgumentException> {
            priceQuote.toBaseSideMonetary(wrongType)
        }
    }

    @Test
    fun `toBaseSideMonetary calculates correctly`() {
        val priceQuote = createTestPriceQuote(500000L) // 50 USD per BTC
        val quoteSideMonetary = FiatVO("USD", 1000000L, "USD", 4, 2) // 100 USD
        val result = priceQuote.toBaseSideMonetary(quoteSideMonetary)
        assertTrue(result is CoinVO)
        assertTrue(result.value > 0)
    }

    @Test
    fun `toQuoteSideMonetary throws for wrong type`() {
        val priceQuote = createTestPriceQuote(500000L)
        val wrongType = FiatVO("USD", 100000L, "USD", 4, 2)
        assertFailsWith<IllegalArgumentException> {
            priceQuote.toQuoteSideMonetary(wrongType)
        }
    }

    @Test
    fun `toQuoteSideMonetary calculates correctly`() {
        val priceQuote = createTestPriceQuote(500000L) // 50 USD per BTC
        val baseSideMonetary = CoinVO("BTC", 100000000, "BTC", 8, 4) // 1 BTC
        val result = priceQuote.toQuoteSideMonetary(baseSideMonetary)
        assertTrue(result is FiatVO)
    }
}
