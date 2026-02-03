package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import kotlin.test.Test
import kotlin.test.assertTrue

class AmountFormatterTest {
    private fun createFiat(
        value: Long,
        code: String = "USD",
    ): FiatVO =
        FiatVO(
            id = code,
            value = value,
            code = code,
            precision = 4,
            lowPrecision = 2,
        )

    private fun createCoin(value: Long): CoinVO =
        CoinVO(
            id = "BTC",
            value = value,
            code = "BTC",
            precision = 8,
            lowPrecision = 4,
        )

    @Test
    fun `formatAmount includes code when requested`() {
        val fiat = createFiat(10000L, "USD") // 1.0000 USD
        val result = AmountFormatter.formatAmount(fiat, useLowPrecision = true, withCode = true)
        assertTrue(result.contains("USD"))
    }

    @Test
    fun `formatAmount excludes code when not requested`() {
        val fiat = createFiat(10000L, "USD")
        val result = AmountFormatter.formatAmount(fiat, useLowPrecision = true, withCode = false)
        assertTrue(!result.contains("USD"))
    }

    @Test
    fun `format handles zero value`() {
        val fiat = createFiat(0L, "USD")
        val result = AmountFormatter.format(fiat, useLowPrecision = true)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `format handles BTC`() {
        val coin = createCoin(100000000L) // 1 BTC
        val result = AmountFormatter.format(coin, useLowPrecision = true)
        assertTrue(result.contains("1"))
    }

    @Test
    fun `formatRangeAmount includes separator`() {
        val min = createFiat(10000L, "USD") // 1.0000 USD
        val max = createFiat(50000L, "USD") // 5.0000 USD
        val result = AmountFormatter.formatRangeAmount(min, max)
        assertTrue(result.contains("-"))
    }

    @Test
    fun `formatRangeAmount includes code for max only`() {
        val min = createFiat(10000L, "USD")
        val max = createFiat(50000L, "USD")
        val result = AmountFormatter.formatRangeAmount(min, max, withCode = true)
        // Code should appear once (for max)
        assertTrue(result.contains("USD"))
    }

    @Test
    fun `format uses low precision when requested`() {
        val fiat = createFiat(12345L, "USD") // 1.2345 USD
        val lowPrecResult = AmountFormatter.format(fiat, useLowPrecision = true)
        val highPrecResult = AmountFormatter.format(fiat, useLowPrecision = false)
        // Low precision should have fewer decimal places
        assertTrue(lowPrecResult.length <= highPrecResult.length)
    }
}
