package network.bisq.mobile.domain.parser

import network.bisq.mobile.domain.toDoubleOrNullLocaleAware

object PriceParser {
    fun parse(value: String): Double {
        try {
            val trimmed = value.replace("%", "").trim()
            if (trimmed.isEmpty()) {
                return 0.0
            }
            // Use locale-aware parsing instead of manual string manipulation
            return trimmed.toDoubleOrNullLocaleAware()
                ?: throw NumberFormatException("Cannot parse '$trimmed' as a number")
        } catch (e: NumberFormatException) {
            throw e
        }
    }

    /**
     * Safe version of parse that returns null instead of throwing exceptions.
     * Uses locale-aware parsing to handle different decimal and thousands separators correctly.
     */
    fun parseOrNull(value: String): Double? {
        return try {
            parse(value)
        } catch (e: Exception) {
            null
        }
    }
}