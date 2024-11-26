package network.bisq.mobile.domain.data.model

data class FiatCurrency(
    val flagImage: String,
    val name: String,
    val code: String,
    val offerCount: Number
)

class Currencies(val currencies: List<FiatCurrency> = listOf()): BaseModel()

interface CurrenciesFactory {
    fun createCurrencies(): Currencies
}

class DefaultCurrenciesFactory : CurrenciesFactory {
    override fun createCurrencies() = Currencies()
}