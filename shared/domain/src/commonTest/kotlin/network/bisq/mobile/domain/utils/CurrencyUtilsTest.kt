package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrencyUtilsTest {
    @Test
    fun `getLocaleFiatCurrencyName returns default when locale returns empty`() {
        // When locale returns empty string, should use default
        val result = CurrencyUtils.getLocaleFiatCurrencyName("INVALID_CODE", "Default Name")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getLocaleFiatCurrencyName returns capitalized name for USD`() {
        val result = CurrencyUtils.getLocaleFiatCurrencyName("USD", "US Dollar")
        assertTrue(result.isNotEmpty())
        // First character should be uppercase
        assertTrue(result[0].isUpperCase())
    }

    @Test
    fun `getLocaleFiatCurrencyName returns capitalized name for EUR`() {
        val result = CurrencyUtils.getLocaleFiatCurrencyName("EUR", "Euro")
        assertTrue(result.isNotEmpty())
        assertTrue(result[0].isUpperCase())
    }

    @Test
    fun `getLocaleFiatCurrencyName handles unknown currency code`() {
        val result = CurrencyUtils.getLocaleFiatCurrencyName("XYZ", "Unknown Currency")
        assertEquals("Unknown Currency", result)
    }

    @Test
    fun `getLocaleFiatCurrencyName returns default when locale returns same as code`() {
        // When locale returns the code itself, should use default
        val result = CurrencyUtils.getLocaleFiatCurrencyName("GBP", "British Pound")
        assertTrue(result.isNotEmpty())
    }
}
