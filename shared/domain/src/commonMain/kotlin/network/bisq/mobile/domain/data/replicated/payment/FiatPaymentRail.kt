package network.bisq.mobile.domain.data.replicated.payment

enum class FiatPaymentRail(
    val countries: List<String>,
    val tradeCurrencies: List<String>,
    val currencyCodes: List<String>
): PaymentRail {
    CUSTOM(emptyList(), emptyList(), emptyList()),

    /*
    SEPA(FiatPaymentRailUtil.getSepaEuroCountries(), emptyList(), emptyList()),
    SEPA_INSTANT(FiatPaymentRailUtil.getSepaEuroCountries(), emptyList(), emptyList()),
    ZELLE(listOf(Country.US), listOf(TradeCurrency.USD), listOf("USD")),
    REVOLUT(FiatPaymentRailUtil.getRevolutCountries(), FiatPaymentRailUtil.getRevolutCurrencies(), emptyList()),
    WISE(FiatPaymentRailUtil.getWiseCountries(), FiatPaymentRailUtil.getWiseCurrencies(), emptyList()),
    NATIONAL_BANK(emptyList(), emptyList(), emptyList()),
    SWIFT(emptyList(), emptyList(), emptyList()),
    F2F(emptyList(), emptyList(), emptyList()),
    ACH_TRANSFER(listOf(Country.US), listOf(TradeCurrency.USD), listOf("USD")),
    PIX(listOf(Country.BR), listOf(TradeCurrency.BRL), listOf("BRL")),
    FASTER_PAYMENTS(listOf(Country.GB), listOf(TradeCurrency.GBP), listOf("GBP")),
    PAY_ID(listOf(Country.AU), listOf(TradeCurrency.AUD), listOf("AUD")),
    US_POSTAL_MONEY_ORDER(listOf(Country.US), listOf(TradeCurrency.USD), listOf("USD")),
    CASH_BY_MAIL(emptyList(), emptyList(), emptyList()),
    STRIKE(listOf(Country.US, Country.SV), listOf(TradeCurrency.USD), listOf("USD")),
    INTERAC_E_TRANSFER(emptyList(), listOf(TradeCurrency.CAD), listOf("CAD")),
    AMAZON_GIFT_CARD(emptyList(),
        listOf(
            TradeCurrency.AUD, TradeCurrency.CAD, TradeCurrency.EUR, TradeCurrency.GBP,
            TradeCurrency.INR, TradeCurrency.JPY, TradeCurrency.SAR, TradeCurrency.SEK,
            TradeCurrency.SGD, TradeCurrency.TRY, TradeCurrency.USD
        ),
        listOf("AUD", "CAD", "EUR", "GBP", "INR", "JPY", "SAR", "SEK", "SGD", "TRY", "USD")
    ),
    CASH_DEPOSIT(emptyList(), emptyList(), emptyList()),
    UPI(emptyList(), listOf(TradeCurrency.INR), listOf("INR")),
    BIZUM(listOf(Country.ES), listOf(TradeCurrency.EUR), listOf("EUR")),
    CASH_APP(listOf(Country.US), listOf(TradeCurrency.USD), listOf("USD"));

    fun supportsCurrency(currencyCode: String): Boolean {
        return currencyCodes.contains(currencyCode)
    }
    */
}
