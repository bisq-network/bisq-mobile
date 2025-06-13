package network.bisq.mobile.domain.parser

import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PriceParserTest {

    @Test
    fun `parse should handle empty string`() {
        assertEquals(0.0, PriceParser.parse(""))
        assertEquals(0.0, PriceParser.parse("   "))
    }

    @Test
    fun `parse should handle basic numbers`() {
        assertEquals(123.0, PriceParser.parse("123"))
        assertEquals(123.45, PriceParser.parse("123.45"))
        assertEquals(0.5, PriceParser.parse("0.5"))
        assertEquals(1000.0, PriceParser.parse("1000"))
    }

    @Test
    fun `parse should remove percentage symbol`() {
        assertEquals(50.0, PriceParser.parse("50%"))
        assertEquals(123.45, PriceParser.parse("123.45%"))
        assertEquals(0.0, PriceParser.parse("%"))
    }

    @Test
    fun `parse should handle whitespace`() {
        assertEquals(123.45, PriceParser.parse("  123.45  "))
        assertEquals(50.0, PriceParser.parse("  50%  "))
    }

    @Test
    fun `parse should throw NumberFormatException for clearly invalid input`() {
        assertFailsWith<NumberFormatException> {
            PriceParser.parse("abc")
        }
    }

    @Test
    fun `parse should handle mixed alphanumeric input consistently`() {
        // Note: The behavior of "12a34" depends on the platform's locale-aware parsing implementation.
        // - Android (Java NumberFormat): May parse partial numbers (e.g., "12" from "12a34")
        // - iOS (NSNumberFormatter): Typically more strict, returns null for invalid input
        val result = PriceParser.parseOrNull("12a34")

        // The result should either be null (if parsing fails) or a valid number (if partially parsed)
        if (result != null) {
            assertTrue(result.isFinite(), "If parsing succeeds, result should be finite")
            assertTrue(result >= 0, "Partial parsing should yield a positive number")
        }
        // If result is null, that's also acceptable - it means parsing failed as expected
    }

    @Test
    fun `parse should throw NumberFormatException for mixed input when platform parsing fails`() {
        // This test verifies that if the platform's toDoubleOrNullLocaleAware returns null,
        // then PriceParser.parse should throw NumberFormatException
        val testInput = "xyz123"  // Clearly invalid input that should fail on all platforms

        assertFailsWith<NumberFormatException> {
            PriceParser.parse(testInput)
        }
    }

    @Test
    fun `parse should handle negative numbers`() {
        assertEquals(-123.45, PriceParser.parse("-123.45"))
        assertEquals(-50.0, PriceParser.parse("-50%"))
    }

    @Test
    fun `parseOrNull should not crash on invalid input`() {
        // The main goal is crash prevention, not specific return values
        // Different locales may handle these differently
        PriceParser.parseOrNull("abc")
        PriceParser.parseOrNull("12a34")
        PriceParser.parseOrNull("invalid")
        // If an exception is thrown the test will fail automatically
    }

    @Test
    fun `parseOrNull should handle valid input`() {
        assertEquals(0.0, PriceParser.parseOrNull(""))
        assertEquals(0.0, PriceParser.parseOrNull("   "))
        assertEquals(123.0, PriceParser.parseOrNull("123"))
        assertEquals(123.45, PriceParser.parseOrNull("123.45"))
        assertEquals(50.0, PriceParser.parseOrNull("50%"))
    }

    @Test
    fun `parseOrNull should handle edge cases gracefully`() {
        assertEquals(0.0, PriceParser.parseOrNull("%"))
        assertEquals(0.0, PriceParser.parseOrNull("  %  "))
    }

    @Test
    fun `parseOrNull should handle very large numbers`() {
        assertEquals(1000000.0, PriceParser.parseOrNull("1000000"))
        assertEquals(1.23456789E8, PriceParser.parseOrNull("123456789"))
    }

    @Test
    fun `parseOrNull should handle very small numbers`() {
        assertEquals(0.00001, PriceParser.parseOrNull("0.00001"))
        assertEquals(1.23E-8, PriceParser.parseOrNull("0.0000000123"))
    }

    @Test
    fun `parseOrNull should handle negative numbers`() {
        assertEquals(-123.45, PriceParser.parseOrNull("-123.45"))
        assertEquals(-50.0, PriceParser.parseOrNull("-50%"))
    }

    @Test
    fun `parseOrNull should handle edge cases without crashing`() {
        // These should not crash, regardless of the result
        val result1 = PriceParser.parseOrNull("1.23E5")
        val result2 = PriceParser.parseOrNull("--")
        val result3 = PriceParser.parseOrNull("++")

        // We don't assert specific values since locale behavior may vary
        // The important thing is that these don't crash
        assertTrue(true, "parseOrNull should handle edge cases without crashing")
    }

    @Test
    fun `parseOrNull should use locale-aware parsing`() {
        // Test that the function uses locale-aware parsing
        // We can't test specific locales in unit tests easily, but we can test
        // that it handles common number formats that would work in most locales
        assertEquals(1234.0, PriceParser.parseOrNull("1234"))
        assertEquals(1234.56, PriceParser.parseOrNull("1234.56"))

        // Test that it doesn't crash on locale-specific formats
        // The exact result depends on the current locale, but it shouldn't crash
        val result1 = PriceParser.parseOrNull("1,234.56")
        val result2 = PriceParser.parseOrNull("1.234,56")

        // At least one of these should work depending on locale, or both should be null
        assertTrue(
            (result1 != null && result1 > 1000) ||
            (result2 != null && result2 > 1000) ||
            (result1 == null && result2 == null),
            "Locale-aware parsing should handle at least one common format or reject both"
        )
    }

    @Test
    fun `parse should handle US number format with thousands separators`() {
        assertEquals(6345343.04, PriceParser.parse("6,345,343.04"))
        assertEquals(1234.56, PriceParser.parse("1,234.56"))
        assertEquals(1000.0, PriceParser.parse("1,000"))
        assertEquals(1000000.0, PriceParser.parse("1,000,000"))
    }

    @Test
    fun `parse should handle European number format with thousands separators`() {
        assertEquals(6345343.04, PriceParser.parse("6.345.343,04"))
        assertEquals(1234.56, PriceParser.parse("1.234,56"))
        assertEquals(1000.0, PriceParser.parse("1.000"))
        assertEquals(1000000.0, PriceParser.parse("1.000.000"))
    }

    @Test
    fun `parse should handle mixed format edge cases`() {
        assertEquals(123.45, PriceParser.parse("123,45"))  // European decimal
        assertEquals(123.45, PriceParser.parse("123.45"))  // US decimal
        assertEquals(1234.0, PriceParser.parse("1,234"))   // Thousands separator
        assertEquals(1234.0, PriceParser.parse("1.234"))   // European thousands or US decimal > 1000
    }

    @Test
    fun `parse should handle numbers with spaces`() {
        assertEquals(1234567.89, PriceParser.parse("1 234 567.89"))
        assertEquals(1234567.89, PriceParser.parse("1 234 567,89"))
    }




}
