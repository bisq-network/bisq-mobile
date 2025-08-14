package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.getLocaleCurrencyName

object CurrencyUtils {
    fun getLocaleFiatCurrencyName(currencyCode: String, defaultCurrencyName: String): String {
        val currencyName = getLocaleCurrencyName(currencyCode).trim()
        return when {
            currencyName.isEmpty() -> defaultCurrencyName
            currencyName.equals(currencyCode, ignoreCase = true) -> defaultCurrencyName
            else -> currencyName
        }
    }
}
