package network.bisq.mobile.domain.parser

object PriceParser {
    fun parse(value: String): Double {
        try {
            val trimmed = value.replace(",", ".")
                .replace("%", "")
                .trim()
            if (trimmed.isEmpty()) {
                return 0.0
            }
            return trimmed.toDouble()
        } catch (e: NumberFormatException) {
            throw e
        }
    }

    /**
     * Safe version of parse that returns null instead of throwing exceptions
     */
    fun parseOrNull(value: String): Double? {
        return try {
            val trimmed = value.replace(",", ".")
                .replace("%", "")
                .trim()
            if (trimmed.isEmpty()) {
                0.0
            } else {
                trimmed.toDouble()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}