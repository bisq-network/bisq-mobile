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

import network.bisq.mobile.data.replicated.common.asset.allFiatCurrencyCodes

enum class FiatPaymentRail(
    val currencyCodes: List<String> = emptyList(),
) : PaymentRailEnum {
    ACH_TRANSFER(listOf("USD")),
    ADVANCED_CASH(FiatPaymentRailUtil.advancedCashCurrencyCodes),
    ALI_PAY(listOf("CNY")),
    AMAZON_GIFT_CARD(FiatPaymentRailUtil.amazonGiftCardCurrencyCodes),
    BIZUM(listOf("EUR")),
    CASH_APP(listOf("USD")),
    CASH_BY_MAIL(allFiatCurrencyCodes),
    CASH_DEPOSIT(allFiatCurrencyCodes),
    CUSTOM(allFiatCurrencyCodes),
    DOMESTIC_WIRE_TRANSFER(listOf("USD")),
    F2F(allFiatCurrencyCodes),
    FASTER_PAYMENTS(listOf("GBP")),
    HAL_CASH(listOf("EUR")),
    IMPS(listOf("INR")),
    INTERAC_E_TRANSFER(listOf("CAD")),
    MERCADO_PAGO(listOf("ARS")),
    MONESE(FiatPaymentRailUtil.moneseCurrencyCodes),
    MONEY_BEAM(listOf("EUR")),
    MONEY_GRAM(FiatPaymentRailUtil.moneyGramCurrencyCodes),
    NATIONAL_BANK(allFiatCurrencyCodes),
    NEFT(listOf("INR")),
    PAY_ID(listOf("AUD")),
    PAYSERA(FiatPaymentRailUtil.payseraCurrencyCodes),
    PERFECT_MONEY(FiatPaymentRailUtil.perfectMoneyCurrencyCodes),
    PIN_4(listOf("PLN")),
    PIX(listOf("BRL")),
    PROMPT_PAY(listOf("THB")),
    REVOLUT(FiatPaymentRailUtil.revolutCurrencies),
    SAME_BANK(allFiatCurrencyCodes),
    SATISPAY(listOf("EUR")),
    SBP(listOf("RUB")),
    SEPA(listOf("EUR")),
    SEPA_INSTANT(listOf("EUR")),
    STRIKE(listOf("USD")),
    SWIFT(allFiatCurrencyCodes),
    SWISH(listOf("SEK")),
    UPHOLD(FiatPaymentRailUtil.upholdCurrencyCodes),
    UPI(listOf("INR")),
    US_POSTAL_MONEY_ORDER(listOf("USD")),
    WECHAT_PAY(listOf("CNY")),
    WISE(FiatPaymentRailUtil.wiseCurrencies),
    WISE_USD(listOf("USD")),
    ZELLE(listOf("USD")),
}
