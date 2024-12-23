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
package network.bisq.mobile.domain.replicated.account.payment_method


object FiatPaymentRailUtil {
    fun getPaymentRails(currencyCode: String): List<FiatPaymentRailEnum> {
        return FiatPaymentRailEnum.entries.filter { fiatPaymentRail ->
            when {
                // For EUR, we don't add NATIONAL_BANK as SEPA is the common payment rail for EUR
                // SWIFT is added to support non-EUR countries offering EUR accounts like Switzerland
                currencyCode == "EUR" && fiatPaymentRail == FiatPaymentRailEnum.NATIONAL_BANK -> false

                // We add NATIONAL_BANK to all others
                fiatPaymentRail == FiatPaymentRailEnum.NATIONAL_BANK -> true

                else -> fiatPaymentRail.supportsCurrency(currencyCode)
            }
        }
    }

    fun getPaymentRailNames(currencyCode: String): List<String> {
        return getPaymentRails(currencyCode).map { it.name }
    }


    val sepaEuroCountries: List<String>
        get() = listOf(
            "AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
            "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA"
        )

    val wiseCountries: List<String>
        // https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from?origin=related-article-2571942
        get() {
            val list: MutableList<String> = ArrayList(
                listOf(
                    "AR", "AU", "BD", "BR", "BG", "CA", "CL", "CN", "CO", "CR", "CZ", "DK", "EG",
                    "GE", "GH", "HK", "HU", "IN", "ID", "IL", "JP", "KE", "MY", "MX", "MA", "NP", "NZ", "NO",
                    "PK", "PH", "PL", "RO", "SG", "ZA", "KR", "LK", "SE", "CH", "TZ", "TH", "TR", "UG", "UA", "AE",
                    "GB", "US", "UY", "VN", "ZM"
                )
            )
            list.addAll(sepaEuroCountries)
            return list
        }

    val wiseCurrencies: List<String>
        // Took all currencies from: https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from
        get() {
            return listOf(
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
                "ZMW"
            )
        }

    val revolutCountries: List<String>
        // https://help.revolut.com/help/wealth/exchanging-money/what-currencies-are-available/what-currencies-are-supported-for-holding-and-exchange/
        get() {
            return listOf(
                "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US"
            )
        }

    val revolutCurrencies: List<String>
        get() {
            return listOf(
                "AED",
                "AUD",
                "BGN",
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
                "ZAR"
            )
        }
}