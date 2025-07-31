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
package network.bisq.mobile.domain.data.replicated.account.payment_method

enum class FiatPaymentRailEnum(val countryCodes: List<String> = emptyList(), val currencyCodes: List<String> = emptyList()) :
    PaymentRailEnum {
    CUSTOM(ArrayList(), ArrayList()),  // Custom defined payment rail by the user
    SEPA(FiatPaymentRailUtil.sepaEuroCountries, listOf("EUR")),
    SEPA_INSTANT(FiatPaymentRailUtil.sepaEuroCountries, listOf("EUR")),
    ZELLE(listOf("US"), listOf("USD")),
    REVOLUT(FiatPaymentRailUtil.revolutCountries, FiatPaymentRailUtil.revolutCurrencies),
    WISE(FiatPaymentRailUtil.wiseCountries, FiatPaymentRailUtil.wiseCurrencies),
    NATIONAL_BANK(ArrayList(), ArrayList()),
    SWIFT,
    F2F,
    ACH_TRANSFER(listOf("US"), listOf("USD")),
    PIX(listOf("BR"), listOf("BRL")),
    FASTER_PAYMENTS(listOf("GB"), listOf("GBP")),
    PAY_ID(listOf("AU"), listOf("AUD")),
    US_POSTAL_MONEY_ORDER(listOf("US"), listOf("USD")),
    CASH_BY_MAIL,
    STRIKE(listOf("US", "SV"), listOf("USD")),
    INTERAC_E_TRANSFER(ArrayList(), listOf("CAD")),
    AMAZON_GIFT_CARD(
        ArrayList(),
        listOf("AUD", "CAD", "EUR", "GBP", "INR", "JPY", "SAR", "SEK", "SGD", "TRY", "USD")
    ),
    CASH_DEPOSIT,
    UPI(ArrayList(), listOf("INR")),
    BIZUM(listOf("ES"), listOf("EUR")),
    CASH_APP(listOf("US"), listOf("USD"));
}

