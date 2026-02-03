package network.bisq.mobile.domain.formatters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberFormatterTest {
    @Test
    fun `format handles zero`() {
        val result = NumberFormatter.format(0.0)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `format handles positive number`() {
        val result = NumberFormatter.format(123.45)
        assertTrue(result.contains("123"))
    }

    @Test
    fun `format handles negative number`() {
        val result = NumberFormatter.format(-123.45)
        assertTrue(result.contains("-123"))
    }

    @Test
    fun `format rounds to 2 decimal places`() {
        val result = NumberFormatter.format(1.2345)
        assertTrue(result.contains("1.23") || result.contains("1,23"))
    }

    @Test
    fun `btcFormat handles zero`() {
        val result = NumberFormatter.btcFormat(0L)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `btcFormat handles 1 BTC`() {
        val result = NumberFormatter.btcFormat(100_000_000L)
        assertTrue(result.contains("1"))
    }

    @Test
    fun `btcFormat handles 1 satoshi`() {
        val result = NumberFormatter.btcFormat(1L)
        assertTrue(result.contains("0.00000001") || result.contains("0,00000001"))
    }

    @Test
    fun `btcFormat handles fractional BTC`() {
        val result = NumberFormatter.btcFormat(50_000_000L)
        assertTrue(result.contains("0.5") || result.contains("0,5"))
    }

    @Test
    fun `btcFormat handles large amount`() {
        val result = NumberFormatter.btcFormat(2_100_000_000_000_000L)
        // 21 million BTC - may have thousands separators
        assertTrue(result.contains("21") && result.contains("000"))
    }
}
