/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.data.replicated.account.payment_method

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailExtensions.supportsCurrency

object FiatPaymentRailUtil {
    fun getPaymentRails(currencyCode: String): List<FiatPaymentRail> =
        FiatPaymentRail.entries.filter { fiatPaymentRail ->
            when {
                // For EUR, we don't add NATIONAL_BANK as SEPA is the common payment rail for EUR
                // SWIFT is added to support non-EUR countries offering EUR accounts like Switzerland
                currencyCode == "EUR" && fiatPaymentRail == FiatPaymentRail.NATIONAL_BANK -> false

                // We add NATIONAL_BANK to all others
                fiatPaymentRail == FiatPaymentRail.NATIONAL_BANK -> true

                // Temporarily we won't show custom payment method
                fiatPaymentRail == FiatPaymentRail.CUSTOM -> false

                // If a payment method does not have any currencyCodes, we add it for all currencies
                fiatPaymentRail.currencyCodes.isEmpty() -> true

                else -> fiatPaymentRail.supportsCurrency(currencyCode)
            }
        }

    fun getPaymentRailNames(currencyCode: String): List<String> = getPaymentRails(currencyCode).map { it.name }

    val sepaEuroCountries: List<String> =
        listOf(
            "AT",
            "BE",
            "BG",
            "CY",
            "DE",
            "EE",
            "FI",
            "FR",
            "GR",
            "HR",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MC",
            "MT",
            "NL",
            "PT",
            "SK",
            "SI",
            "ES",
            "AD",
            "SM",
            "VA",
        )

    val sepaNonEuroCountries: List<String> =
        listOf(
            "CZ",
            "DK",
            "GB",
            "HU",
            "PL",
            "RO",
            "SE",
            "IS",
            "NO",
            "LI",
            "CH",
            "JE",
            "GI",
        )

    val allSepaCountryCodes = (sepaEuroCountries + sepaNonEuroCountries).distinct().sorted()

    // Took all currencies from: https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from
    val wiseCurrencies: List<String> =
        listOf(
            "AED",
            "ARS",
            "AUD",
            "BDT",
            "BGN",
            "BRL",
            "BWP",
            "CAD",
            "CHF",
            "CLP",
            "CNY",
            "COP",
            "CRC",
            "CZK",
            "DKK",
            "EGP",
            "EUR",
            "FJD",
            "GEL",
            "GHS",
            "GBP",
            "HKD",
            "HUF",
            "IDR",
            "ILS",
            "INR",
            "JPY",
            "KES",
            "KRW",
            "LKR",
            "MAD",
            "MXN",
            "MYR",
            "NOK",
            "NPR",
            "NZD",
            "PHP",
            "PKR",
            "PLN",
            "RON",
            "SEK",
            "SGD",
            "THB",
            "TRY",
            "UAH",
            "UGX",
            "USD",
            "UYU",
            "VND",
            "ZAR",
            "ZMW",
        )

    val advancedCashCurrencyCodes: List<String> = listOf("BRL", "EUR", "GBP", "KZT", "RUB", "UAH", "USD")

    val moneseCurrencyCodes: List<String> = listOf("EUR", "GBP", "RON")

    val payseraCurrencyCodes: List<String> =
        listOf(
            "AUD",
            "BYN",
            "CAD",
            "CHF",
            "CNY",
            "CZK",
            "DKK",
            "EUR",
            "GBP",
            "GEL",
            "HKD",
            "HUF",
            "ILS",
            "INR",
            "JPY",
            "KZT",
            "MXN",
            "NOK",
            "NZD",
            "PHP",
            "PLN",
            "RON",
            "RSD",
            "RUB",
            "SEK",
            "SGD",
            "THB",
            "TRY",
            "USD",
            "ZAR",
        )

    val perfectMoneyCurrencyCodes: List<String> = listOf("EUR", "USD")

    val upholdCurrencyCodes: List<String> =
        listOf(
            "AED",
            "ARS",
            "AUD",
            "BRL",
            "CAD",
            "CHF",
            "CNY",
            "DKK",
            "EUR",
            "GBP",
            "HKD",
            "ILS",
            "INR",
            "JPY",
            "KES",
            "MXN",
            "NOK",
            "NZD",
            "PHP",
            "PLN",
            "SEK",
            "SGD",
            "USD",
        )

    val amazonGiftCardCurrencyCodes: List<String> = listOf("AUD", "CAD", "EUR", "GBP", "INR", "JPY", "SAR", "SEK", "SGD", "TRY", "USD")

    val moneyGramCurrencyCodes: List<String> =
        listOf(
            "AED",
            "ARS",
            "AUD",
            "BND",
            "CAD",
            "CHF",
            "CZK",
            "DKK",
            "EUR",
            "FJD",
            "GBP",
            "HKD",
            "HUF",
            "IDR",
            "ILS",
            "INR",
            "JPY",
            "KRW",
            "KWD",
            "LKR",
            "MAD",
            "MGA",
            "MXN",
            "MYR",
            "NOK",
            "NZD",
            "OMR",
            "PEN",
            "PGK",
            "PHP",
            "PKR",
            "PLN",
            "SAR",
            "SBD",
            "SCR",
            "SEK",
            "SGD",
            "THB",
            "TOP",
            "TRY",
            "TWD",
            "USD",
            "VND",
            "VUV",
            "WST",
            "XOF",
            "XPF",
            "ZAR",
        )

    val revolutCurrencies: List<String> =
        listOf(
            "AED",
            "AUD",
            "CAD",
            "CHF",
            "CZK",
            "DKK",
            "EUR",
            "GBP",
            "HKD",
            "HUF",
            "ILS",
            "ISK",
            "JPY",
            "MAD",
            "MXN",
            "NOK",
            "NZD",
            "PLN",
            "QAR",
            "RON",
            "RSD",
            "RUB",
            "SAR",
            "SEK",
            "SGD",
            "THB",
            "TRY",
            "USD",
            "ZAR",
        )
}
