package network.bisq.mobile.domain.offerbook

import network.bisq.mobile.client.replicated_model.common.currency.Market

interface OfferbookServiceFacade {
    val markets: List<Market>
    fun initialize()
    fun dispose()
    fun resume()
    fun selectMarket(market: Market)

    companion object {
        val mainCurrencies: List<String> =
            listOf("usd", "eur", "gbp", "cad", "aud", "rub", "cny", "inr", "ngn")
    }
}