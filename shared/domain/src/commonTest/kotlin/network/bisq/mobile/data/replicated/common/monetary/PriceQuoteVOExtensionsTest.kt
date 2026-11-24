package network.bisq.mobile.data.replicated.common.monetary

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.asDouble
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toDouble
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
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

    // bisq2 PriceQuote: toBaseSideMonetary rounds half up to whole base units, toQuoteSideMonetary
    // multiplies exactly and truncates. The take-offer rule must convert like the trusted node.

    @Test
    fun `toBaseSideMonetary rounds half up to whole satoshis like bisq2`() {
        // 100 EUR at 60001 EUR/BTC = 166663.889 sats
        val quote = createTestPriceQuote(60_001_0000L)
        assertEquals(166_664L, quote.toBaseSideMonetary(FiatVO("USD", 100_0000L, "USD", 4, 2)).value)
    }

    @Test
    fun `toBaseSideMonetary keeps full precision above eight significant digits`() {
        // 123456.7891 USD at 1.0000 USD/BTC = 12345678910000 sats (14 digits)
        val quote = createTestPriceQuote(1_0000L)
        assertEquals(12_345_678_910_000L, quote.toBaseSideMonetary(FiatVO("USD", 1_234_567_891L, "USD", 4, 2)).value)
    }

    @Test
    fun `toQuoteSideMonetary multiplies exactly and truncates like bisq2`() {
        // 166664 sats at 60301.0050 USD/BTC = 100.500048... USD; an 8-digit product would round it
        val quote = createTestPriceQuote(60_301_0050L)
        assertEquals(1_005_000L, quote.toQuoteSideMonetary(CoinVO("BTC", 166_664L, "BTC", 8, 4)).value)
        // 99999999 sats at 99999.9999 = 99999.99890000... USD, truncated
        val highQuote = createTestPriceQuote(99_999_9999L)
        assertEquals(999_999_989L, highQuote.toQuoteSideMonetary(CoinVO("BTC", 99_999_999L, "BTC", 8, 4)).value)
    }
}
