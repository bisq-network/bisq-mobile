package network.bisq.mobile.domain.utils

import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The seller reputation rule, mirrored from bisq2 `BisqEasyTradeAmountLimits` and its test with the
 * examples of docs/specifications/trade/bisq-easy-seller-reputation-limit.md. Both copies must give the
 * same numbers, otherwise the maker or the buyer's node rejects the amount this app offers.
 */
class BisqEasyTradeAmountLimitsRuleTest {
    private val limits = TradeAmountLimitsVO.DEFAULT

    private fun quote(
        price: Double,
        currencyCode: String,
    ): PriceQuoteVO = with(PriceQuoteVOFactory) { fromPrice(price, "BTC", currencyCode) }

    private fun usd(faceValue: Double) = with(FiatVOFactory) { fromFaceValue(faceValue, "USD") }

    private fun fiat(
        faceValue: Double,
        currencyCode: String,
    ) = with(FiatVOFactory) { fromFaceValue(faceValue, currencyCode) }

    @Test
    fun `required score is 200 per usd`() {
        assertEquals(120_000, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usd(600.0), limits))
        assertEquals(1_200, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usd(6.0), limits))
    }

    @Test
    fun `required score rounds the usd amount half up`() {
        assertEquals(20_000, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usd(100.49), limits))
        assertEquals(20_200, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usd(100.5), limits))
    }

    @Test
    fun `required score from quotes uses the usd value`() {
        // 500 EUR = 0.01 BTC = 600 USD
        val required =
            BisqEasyTradeAmountLimits.getRequiredReputationScore(
                quote(50_000.0, "EUR"),
                quote(60_000.0, "USD"),
                fiat(500.0, "EUR"),
                limits,
            )
        assertEquals(120_000, required)
    }

    @Test
    fun `usd amount from score is rounded down`() {
        assertEquals(usd(10.0).value, BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(2_000, limits).value)
        assertEquals(usd(9.0).value, BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(1_999, limits).value)
    }

    @Test
    fun `usd amount from score never requires more than the score`() {
        for (score in 0L..20_000L) {
            val usdAmount = BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(score, limits)
            val required = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usdAmount, limits)
            assertTrue(required <= score, "score=$score required=$required")
        }
    }

    @Test
    fun `tolerance is five percent for high scores`() {
        assertEquals(20_000, BisqEasyTradeAmountLimits.withTolerance(19_048, limits))
        assertEquals(19_999, BisqEasyTradeAmountLimits.withTolerance(19_047, limits))
    }

    @Test
    fun `tolerance covers one usd for positive low scores`() {
        val expected =
            mapOf(
                0L to 0L,
                1_200L to 1_400L,
                1_300L to 1_500L,
                2_400L to 2_600L,
                3_999L to 4_199L,
                4_000L to 4_200L,
                4_010L to 4_211L,
            )
        expected.forEach { (score, tolerated) ->
            assertEquals(tolerated, BisqEasyTradeAmountLimits.withTolerance(score, limits), "score=$score")
        }
    }

    @Test
    fun `tolerance uses the minimum tolerance from the config`() {
        val custom = limits.copy(minimumReputationScoreTolerance = 300)
        assertEquals(1_500, BisqEasyTradeAmountLimits.withTolerance(1_200, custom))
    }

    @Test
    fun `required score is covered up to the score with tolerance`() {
        assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(0, 0, limits))
        assertFalse(BisqEasyTradeAmountLimits.isCoveredWithTolerance(0, 1, limits))
        assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(1_300, 1_500, limits))
        assertFalse(BisqEasyTradeAmountLimits.isCoveredWithTolerance(1_300, 1_501, limits))
        assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(100_000, 105_000, limits))
        assertFalse(BisqEasyTradeAmountLimits.isCoveredWithTolerance(100_000, 105_001, limits))
    }

    @Test
    fun `tolerance covers the legacy maximum of a seller maker which needs more than the score`() {
        // A 2.1.13 maker with score 79902 derived 400 USD, which require 80000
        val required = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usd(400.0), limits)
        assertEquals(80_000, required)
        assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(79_902, required, limits))
        assertEquals(usd(399.0).value, BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(79_902, limits).value)
    }

    @Test
    fun `buyers max amount is the sellers maximum but not below the offers minimum`() {
        // The seller's maximum limits the offer
        assertEquals(399L, BisqEasyTradeAmountLimits.getBuyersMaxQuoteSideAmount(50L, 400L, 399L))
        // The offer's maximum is below the seller's maximum
        assertEquals(300L, BisqEasyTradeAmountLimits.getBuyersMaxQuoteSideAmount(50L, 300L, 399L))
        // The seller's maximum is below the offer's minimum, which the tolerance covers
        assertEquals(3_996_000L, BisqEasyTradeAmountLimits.getBuyersMaxQuoteSideAmount(3_996_000L, 4_000_000L, 3_990_000L))
    }

    @Test
    fun `reputation based amount is lowered when rounding up needs more than the score`() {
        // 6 USD are 5.52 EUR. Rounded to 6 EUR they are 6.52 USD, which count as 7 USD and require 1400.
        val amount =
            BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(quote(55_200.0, "EUR"), quote(60_000.0, "USD"), 1_200, limits)
        assertEquals(fiat(5.0, "EUR").value, amount.value)
        assertEquals("EUR", amount.code)
    }

    @Test
    fun `reputation based amount keeps a whole unit reduced by the conversion through btc`() {
        // 6 USD converted to BTC and back are 5.9995 USD, which count as 6 USD and require 1200
        val usdQuote = quote(83_875.1546, "USD")
        val amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(usdQuote, usdQuote, 1_200, limits)
        assertEquals(usd(6.0).value, amount.value)
    }

    @Test
    fun `low score maximum across a usd rounding boundary is covered by the tolerance at a slightly different price`() {
        val usdQuote = quote(83_875.15, "USD")
        val amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(quote(73_808.56, "EUR"), usdQuote, 2_400, limits)
        assertEquals(fiat(11.0, "EUR").value, amount.value)

        // The maker's EUR price is 0.05% lower
        val required = BisqEasyTradeAmountLimits.getRequiredReputationScore(quote(73_771.66, "EUR"), usdQuote, amount, limits)
        assertEquals(2_600, required)
        assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(2_400, required, limits))
    }

    @Test
    fun `reputation based amount is in whole units and needs at most the score`() {
        val markets =
            listOf(
                Triple("EUR", 55_200.0, 60_000.0),
                Triple("EUR", 50_000.0, 60_000.0),
                Triple("GBP", 47_200.0, 60_000.0),
                Triple("KWD", 18_400.0, 60_000.0),
                Triple("CHF", 53_000.0, 60_000.0),
                Triple("BRL", 434_533.5, 83_875.1546),
                Triple("JPY", 13_333_584.5, 83_875.1546),
                Triple("USD", 60_000.0, 60_000.0),
                Triple("USD", 83_875.1546, 83_875.1546),
            )
        markets.forEach { (currencyCode, btcPrice, btcUsdPrice) ->
            val fiatQuote = quote(btcPrice, currencyCode)
            val usdQuote = quote(btcUsdPrice, "USD")
            // Every score up to 4000 (where the minimum tolerance matters) plus a coarse sweep above
            val scores = (0L..4_000L) + (4_000L..20_000L step 37)
            scores.forEach { score ->
                val amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(fiatQuote, usdQuote, score, limits)
                val required = BisqEasyTradeAmountLimits.getRequiredReputationScore(fiatQuote, usdQuote, amount, limits)
                val context = "$currencyCode score=$score amount=${amount.value} required=$required"
                val oneUnit = 10.0.pow(amount.precision).toLong()

                assertEquals(0L, amount.value % oneUnit, context)
                assertTrue(required <= score, context)
                assertTrue(amount.value <= convertedUsdAllowanceRoundedToWholeUnits(fiatQuote, usdQuote, score).value, context)
            }
        }
    }

    private fun convertedUsdAllowanceRoundedToWholeUnits(
        fiatQuote: PriceQuoteVO,
        usdQuote: PriceQuoteVO,
        score: Long,
    ) = fiatQuote.toQuoteSideMonetary(usdQuote.toBaseSideMonetary(BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(score, limits))).round(0)
}
