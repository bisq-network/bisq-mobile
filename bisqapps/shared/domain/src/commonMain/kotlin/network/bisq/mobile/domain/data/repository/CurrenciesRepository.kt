package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.model.Currencies
import network.bisq.mobile.domain.data.model.FiatCurrency

// TODO:
// androidNode will populate List<FiatCurrency> from bisq2 libs
// xClients will populate List<FiatCurrency> via API
open class CurrenciesRepository : SingleObjectRepository<Currencies>() {
    init {
        runBlocking {
            val currencies = Currencies(
                currencies = listOf(
                    FiatCurrency(
                        flagImage = "currency_aed.png",
                        name = "United Arab Emirates Dirham",
                        code = "aed",
                        offerCount = 9
                    ),
                    FiatCurrency(flagImage = "currency_ars.png", name = "Argentine Peso", code = "ars", offerCount = 0),
                    FiatCurrency(
                        flagImage = "currency_aud.png",
                        name = "Australian Dollar",
                        code = "aud",
                        offerCount = 12
                    ),
                    FiatCurrency(flagImage = "currency_eur.png", name = "Euro", code = "eur", offerCount = 66),
                    FiatCurrency(
                        flagImage = "currency_gbp.png",
                        name = "British Pound Sterling",
                        code = "gbp",
                        offerCount = 3
                    ),

                    FiatCurrency(flagImage = "currency_jpy.png", name = "Japanese Yen", code = "jpy", offerCount = 2),

                    FiatCurrency(flagImage = "currency_qar.png", name = "Qatari Rial", code = "qar", offerCount = 4),
                    FiatCurrency(flagImage = "currency_sek.png", name = "Swedish Krona", code = "sek", offerCount = 18),
                    FiatCurrency(
                        flagImage = "currency_sgd.png",
                        name = "Singapore Dollar",
                        code = "sgd",
                        offerCount = 16
                    ),
                    FiatCurrency(flagImage = "currency_usd.png", name = "US Dollar", code = "usd", offerCount = 62),
                )
            )
            create(currencies)
        }
    }
}