package network.bisq.mobile.domain.data.replicated.common.monetary

import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.asDouble
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.decimalMode
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.toDouble
import kotlin.test.Test
import kotlin.test.assertEquals

class MonetaryVOExtensionsTest {
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
        assertEquals(1.0, coin.toDouble(100000000L))
    }

    @Test
    fun `toDouble converts partial BTC correctly`() {
        val coin = createCoin(50000000L) // 0.5 BTC
        assertEquals(0.5, coin.toDouble(50000000L))
    }

    @Test
    fun `toDouble converts small amounts correctly`() {
        val coin = createCoin(1L) // 1 satoshi
        assertEquals(0.00000001, coin.toDouble(1L))
    }

    @Test
    fun `asDouble returns correct value for coin`() {
        val coin = createCoin(100000000L) // 1 BTC
        assertEquals(1.0, coin.asDouble())
    }

    @Test
    fun `asDouble returns correct value for fiat`() {
        val fiat = createFiat(10000L) // 1.0000 USD
        assertEquals(1.0, fiat.asDouble())
    }

    @Test
    fun `asDouble handles zero value`() {
        val coin = createCoin(0L)
        assertEquals(0.0, coin.asDouble())
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
        assertEquals(1.2345, fiat.toDouble(12345L))
    }
}
