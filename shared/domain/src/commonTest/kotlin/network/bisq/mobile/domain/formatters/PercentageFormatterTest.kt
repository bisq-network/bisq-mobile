package network.bisq.mobile.domain.formatters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PercentageFormatterTest {
    @Test
    fun `format returns percentage with symbol by default`() {
        val result = PercentageFormatter.format(0.5)
        assertTrue(result.contains("%"))
    }

    @Test
    fun `format returns percentage without symbol when specified`() {
        val result = PercentageFormatter.format(0.5, withSymbol = false)
        assertTrue(!result.contains("%"))
    }

    @Test
    fun `format handles zero`() {
        val result = PercentageFormatter.format(0.0)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `format handles 100 percent`() {
        val result = PercentageFormatter.format(1.0)
        assertTrue(result.contains("100"))
    }

    @Test
    fun `format handles negative percentage`() {
        val result = PercentageFormatter.format(-0.05)
        assertTrue(result.contains("-5"))
    }

    @Test
    fun `format handles small decimal percentage`() {
        val result = PercentageFormatter.format(0.0125)
        assertTrue(result.contains("1.25") || result.contains("1,25"))
    }

    @Test
    fun `format rounds to 2 decimal places`() {
        val result = PercentageFormatter.format(0.12345, withSymbol = false)
        // 12.345% should round to 12.34% or 12.35% depending on rounding mode
        assertTrue(result.contains("12.34") || result.contains("12,34") || result.contains("12.35") || result.contains("12,35"))
    }
}
