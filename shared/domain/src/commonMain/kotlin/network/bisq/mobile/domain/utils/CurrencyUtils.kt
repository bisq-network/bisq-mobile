package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.getLocaleCurrencyName

object CurrencyUtils {
    fun getLocaleFiatCurrencyName(currencyCode: String, defaultCurrencyName: String): String {
        val currencyName = getLocaleCurrencyName(currencyCode).trim()
        return when {
            currencyName.isEmpty() -> defaultCurrencyName
            currencyName.equals(currencyCode, ignoreCase = true) -> defaultCurrencyName
            else -> capitalizeFirstChar(currencyName)
        }
    }

    /**
     * Capitalizes the first character of each word in a string (title case).
     * This ensures consistent currency name formatting across all locales.
     *
     * Examples:
     * - "rublo ruso" -> "Rublo Ruso"
     * - "US Dollar" -> "US Dollar" (already capitalized)
     * - "euro" -> "Euro"
     * - "british pound sterling" -> "British Pound Sterling"
     */
    private fun capitalizeFirstChar(text: String): String {
        return if (text.isEmpty()) {
            text
        } else {
            text.split(" ").joinToString(" ") { word ->
                if (word.isEmpty()) {
                    word
                } else {
                    word.first().uppercaseChar() + word.drop(1)
                }
            }
        }
    }
}
