package network.bisq.mobile.domain.parser

import network.bisq.mobile.domain.toDoubleOrNullLocaleAware

object PriceParser {
    fun parse(value: String): Double {
        try {
            val trimmed = value.replace("%", "").trim()
            if (trimmed.isEmpty()) {
                return 0.0
            }

            val needsNormalization = needsNormalization(trimmed)
            if (!needsNormalization) {
                // For simple cases, try locale-aware parsing directly
                trimmed.toDoubleOrNullLocaleAware()?.let { return it }
            }

            // For complex cases or when simple parsing fails, normalize first
            val normalized = normalizeNumberFormat(trimmed)
            return normalized.toDoubleOrNullLocaleAware()
                ?: throw NumberFormatException("Cannot parse '$trimmed' as a number")
        } catch (e: NumberFormatException) {
            throw e
        }
    }

    /**
     * Determines if a number string needs normalization based on its format.
     * Returns true for complex cases that likely need normalization.
     */
    private fun needsNormalization(input: String): Boolean {
        val cleaned = input.replace(" ", "")

        // Count separators
        val commaCount = cleaned.count { it == ',' }
        val dotCount = cleaned.count { it == '.' }

        return when {
            input.contains(" ") -> true
            commaCount > 1 || dotCount > 1 -> true
            commaCount > 0 && dotCount > 0 -> true
            // Single separator but in a position that suggests thousands separator
            // (more than 3 digits before the separator suggests thousands format)
            commaCount == 1 -> {
                val commaIndex = cleaned.indexOf(',')
                val beforeComma = cleaned.substring(0, commaIndex)
                val afterComma = cleaned.substring(commaIndex + 1)
                // If there are more than 3 digits before comma, or exactly 3 digits after comma,
                // it's likely a thousands separator that needs normalization
                // Also normalize if it's a short decimal (like "123,45") to ensure proper parsing
                beforeComma.length > 3 || afterComma.length == 3 || afterComma.length <= 2
            }

            dotCount == 1 -> {
                val dotIndex = cleaned.indexOf('.')
                val beforeDot = cleaned.substring(0, dotIndex)
                val afterDot = cleaned.substring(dotIndex + 1)
                // If there are more than 3 digits before dot and exactly 3 digits after,
                // it might be European thousands format (e.g., "1234.567")
                // Also normalize if it's exactly 3 digits after dot (like "1.000")
                (beforeDot.length > 3 && afterDot.length == 3) || afterDot.length == 3
            }

            else -> false
        }
    }

    /**
     * Normalizes number format by detecting decimal separator position and
     * converting to a standard format that locale-aware parsing can handle.
     *
     * Examples:
     * "6,345,343.04" -> "6345343.04" (US format)
     * "6.345.343,04" -> "6345343.04" (European format)
     * "1,234" -> "1234" (could be thousands or decimal - assume thousands if no decimal part)
     */
    private fun normalizeNumberFormat(input: String): String {
        if (input.isEmpty()) return input
        val cleaned = input.replace(" ", "")

        if (!cleaned.contains(',') && !cleaned.contains('.')) {
            return cleaned
        }

        // Find the last occurrence of comma or dot - this is likely the decimal separator
        val lastCommaIndex = cleaned.lastIndexOf(',')
        val lastDotIndex = cleaned.lastIndexOf('.')

        return when {
            // No decimal separators
            lastCommaIndex == -1 && lastDotIndex == -1 -> cleaned
            // Only commas - determine if last comma is decimal or thousands separator
            lastDotIndex == -1 -> {
                val afterComma = cleaned.substring(lastCommaIndex + 1)
                if (afterComma.length <= 2) {
                    // Likely decimal separator (e.g., "123,45")
                    val beforeDecimal = cleaned.substring(0, lastCommaIndex).replace(",", "")
                    "$beforeDecimal.$afterComma"
                } else {
                    // Likely thousands separator (e.g., "1,000")
                    cleaned.replace(",", "")
                }
            }
            // Only dots - determine if last dot is decimal or thousands separator
            lastCommaIndex == -1 -> {
                val afterDot = cleaned.substring(lastDotIndex + 1)
                if (afterDot.length <= 2) {
                    // Likely decimal separator (e.g., "123.45")
                    val beforeDecimal = cleaned.substring(0, lastDotIndex).replace(".", "")
                    "$beforeDecimal.$afterDot"
                } else {
                    // Likely thousands separator (e.g., "1.000")
                    cleaned.replace(".", "")
                }
            }
            // Both commas and dots - the rightmost one is decimal separator
            lastCommaIndex > lastDotIndex -> {
                // Comma is decimal separator (European format like "1.234.567,89")
                val beforeDecimal = cleaned.substring(0, lastCommaIndex).replace(",", "").replace(".", "")
                val afterDecimal = cleaned.substring(lastCommaIndex + 1)
                "$beforeDecimal.$afterDecimal"
            }
            else -> {
                // Dot is decimal separator (US format like "1,234,567.89")
                val beforeDecimal = cleaned.substring(0, lastDotIndex).replace(",", "").replace(".", "")
                val afterDecimal = cleaned.substring(lastDotIndex + 1)
                "$beforeDecimal.$afterDecimal"
            }
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