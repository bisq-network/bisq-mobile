package network.bisq.mobile.domain.data.replicated.common.monetary

import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.asDouble
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.decimalMode
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.toDouble
import kotlin.test.Test
import kotlin.test.assertEquals

class MonetaryVOExtensionsTest {
    // Tolerance for floating-point comparisons
    private val tolerance = 1e-9

    // Helper to create a CoinVO for testing
    private fun createCoin(
        value: Long,
        precision: Int = 8,
    ): CoinVO =
        CoinVO(
            id = "BTC",
            value = value,
            code = "BTC",
            precision = precision,
            lowPrecision = precision,
        )

    // Helper to create a FiatVO for testing
    private fun createFiat(
        value: Long,
        code: String = "USD",
        precision: Int = 4,
    ): FiatVO =
        FiatVO(
            id = code,
            value = value,
            code = code,
            precision = precision,
            lowPrecision = 2,
        )

    @Test
    fun `toDouble converts satoshis to BTC correctly`() {
        val coin = createCoin(100000000L) // 1 BTC in satoshis
        assertEquals(1.0, coin.toDouble(100000000L), tolerance)
    }

    @Test
    fun `toDouble converts partial BTC correctly`() {
        val coin = createCoin(50000000L) // 0.5 BTC
        assertEquals(0.5, coin.toDouble(50000000L), tolerance)
    }

    @Test
    fun `toDouble converts small amounts correctly`() {
        val coin = createCoin(1L) // 1 satoshi
        assertEquals(0.00000001, coin.toDouble(1L), tolerance)
    }

    @Test
    fun `asDouble returns correct value for coin`() {
        val coin = createCoin(100000000L) // 1 BTC
        assertEquals(1.0, coin.asDouble(), tolerance)
    }

    @Test
    fun `asDouble returns correct value for fiat`() {
        val fiat = createFiat(10000L) // 1.0000 USD
        assertEquals(1.0, fiat.asDouble(), tolerance)
    }

    @Test
    fun `asDouble handles zero value`() {
        val coin = createCoin(0L)
        assertEquals(0.0, coin.asDouble(), tolerance)
    }

    @Test
    fun `decimalMode has correct precision for coin`() {
        val coin = createCoin(100000000L)
        assertEquals(8L, coin.decimalMode.decimalPrecision)
    }

    @Test
    fun `decimalMode has correct precision for fiat`() {
        val fiat = createFiat(10000L)
        assertEquals(4L, fiat.decimalMode.decimalPrecision)
    }

    @Test
    fun `toDouble works with different precision values`() {
        val fiat = createFiat(12345L, "USD", 4) // 1.2345 USD
        assertEquals(1.2345, fiat.toDouble(12345L), tolerance)
    }

    @Test
    fun `toDouble handles large values`() {
        val coin = createCoin(2100000000000000L) // 21 million BTC
        assertEquals(21000000.0, coin.asDouble(), tolerance)
    }

    @Test
    fun `toDouble handles negative values`() {
        val coin = createCoin(-100000000L) // -1 BTC
        assertEquals(-1.0, coin.asDouble(), tolerance)
    }

    @Test
    fun `asDouble handles different fiat currencies`() {
        val eur = createFiat(50000L, "EUR", 4) // 5.0000 EUR
        assertEquals(5.0, eur.asDouble(), tolerance)
    }

    @Test
    fun `decimalMode uses ROUND_HALF_AWAY_FROM_ZERO`() {
        val coin = createCoin(100000000L)
        assertEquals(
            com.ionspin.kotlin.bignum.decimal.RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
            coin.decimalMode.roundingMode,
        )
    }
}
