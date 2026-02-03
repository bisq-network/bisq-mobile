package network.bisq.mobile.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class PercentageParserTest {
    private val tolerance = 1e-9

    @Test
    fun `parse returns 0 for empty string`() {
        assertEquals(0.0, PercentageParser.parse(""), tolerance)
    }

    @Test
    fun `parse returns 0 for whitespace only`() {
        assertEquals(0.0, PercentageParser.parse("   "), tolerance)
    }

    @Test
    fun `parse handles simple percentage`() {
        assertEquals(0.5, PercentageParser.parse("50"), tolerance)
    }

    @Test
    fun `parse handles percentage with percent sign`() {
        assertEquals(0.25, PercentageParser.parse("25%"), tolerance)
    }

    @Test
    fun `parse handles percentage with spaces and percent sign`() {
        assertEquals(0.1, PercentageParser.parse("  10 %  "), tolerance)
    }

    @Test
    fun `parse handles decimal percentage`() {
        assertEquals(0.125, PercentageParser.parse("12.5"), tolerance)
    }

    @Test
    fun `parse handles comma as decimal separator`() {
        assertEquals(0.125, PercentageParser.parse("12,5"), tolerance)
    }

    @Test
    fun `parse handles negative percentage`() {
        assertEquals(-0.05, PercentageParser.parse("-5"), tolerance)
    }

    @Test
    fun `parse handles zero`() {
        assertEquals(0.0, PercentageParser.parse("0"), tolerance)
    }

    @Test
    fun `parse handles 100 percent`() {
        assertEquals(1.0, PercentageParser.parse("100"), tolerance)
    }

    @Test
    fun `parse returns 0 for invalid input`() {
        assertEquals(0.0, PercentageParser.parse("abc"), tolerance)
    }

    @Test
    fun `parse returns 0 for special characters`() {
        assertEquals(0.0, PercentageParser.parse("!@#"), tolerance)
    }
}
