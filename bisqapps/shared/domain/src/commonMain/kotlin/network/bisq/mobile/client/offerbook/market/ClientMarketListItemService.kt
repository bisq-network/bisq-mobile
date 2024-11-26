package network.bisq.mobile.client.offerbook.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.service.Polling
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.offerbook.market.MarketListItem
import network.bisq.mobile.utils.Logging


class ClientMarketListItemService(private val apiGateway: OfferbookApiGateway) : LifeCycleAware,
    Logging {

    // Properties
    private val _marketListItems: MutableList<MarketListItem> = mutableListOf()
    val marketListItems: List<MarketListItem> get() = _marketListItems

    // Misc
    private var polling = Polling(1000) { updateNumOffers() }
    private var marketListItemsRequested = false

    // Life cycle
    override fun activate() {
        // As markets are rather static we apply the default markets immediately.
        // Markets would only change if we get new markets added to the market price server,
        // which happens rarely.
        val defaultMarkets = Json.decodeFromString<List<Market>>(DEFAULT_MARKETS)
        fillMarketListItems(defaultMarkets, emptyMap())

        if (marketListItemsRequested) {
            CoroutineScope(BackgroundDispatcher).launch {
                try {
                    // TODO we might combine that api call to avoid 2 separate calls.
                    val numOffersByMarketCode: Map<String, Int> =
                        apiGateway.getNumOffersByMarketCode()
                    val markets = apiGateway.getMarkets()
                    fillMarketListItems(markets, numOffersByMarketCode)
                    marketListItemsRequested = true
                } catch (e: Exception) {
                    log.e("Error at API request", e)
                }
            }
        }
        polling.start()
    }

    override fun deactivate() {
        polling.stop()
    }

    // Private
    private fun fillMarketListItems(
        markets: List<Market>,
        numOffersByMarketCode: Map<String, Int>
    ) {
        val list = markets.map { marketDto ->
            val market = Market(
                marketDto.baseCurrencyCode,
                marketDto.quoteCurrencyCode,
                marketDto.baseCurrencyName,
                marketDto.quoteCurrencyName,
            )

            val marketListItem = MarketListItem(market)
            val numOffers = numOffersByMarketCode[marketDto.quoteCurrencyCode] ?: 0
            marketListItem.setNumOffers(numOffers)
            marketListItem
        }
        _marketListItems.addAll(list)
    }

    private fun updateNumOffers() {
        CoroutineScope(BackgroundDispatcher).launch {
            try {
                val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()
                marketListItems.map { marketListItem ->
                    val numOffers =
                        numOffersByMarketCode[marketListItem.market.quoteCurrencyCode] ?: 0
                    marketListItem.setNumOffers(numOffers)
                    marketListItem
                }
            } catch (e: Exception) {
                log.e("Error at API request", e)
            }
        }
    }
}