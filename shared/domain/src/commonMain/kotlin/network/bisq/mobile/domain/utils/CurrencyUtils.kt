package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.getLocaleCurrencyName

object CurrencyUtils {
    fun getLocaleFiatCurrencyName(currencyCode: String, defaultCurrencyName: String): String {
        val currencyName = getLocaleCurrencyName(currencyCode)

        if(currencyName.isEmpty()) {
            return defaultCurrencyName
        }

        return currencyName
    }
}
