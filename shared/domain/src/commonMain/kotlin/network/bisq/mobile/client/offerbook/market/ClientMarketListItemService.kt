package network.bisq.mobile.client.offerbook.market

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.service.Polling
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.utils.Logging


class ClientMarketListItemService(private val apiGateway: OfferbookApiGateway) : LifeCycleAware,
    Logging {

    // Properties
    private val _marketListItems: MutableList<MarketListItem> = mutableListOf()
    val marketListItems: List<MarketListItem> get() = _marketListItems

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null
    private var polling = Polling(10000) { updateNumOffers() }
    private var marketListItemsRequested = false

    // Life cycle
    override fun activate() {
        // As markets are rather static we apply the default markets immediately.
        // Markets would only change if we get new markets added to the market price server,
        // which happens rarely.
        val defaultMarkets = Json.decodeFromString<List<Market>>(DEFAULT_MARKETS)
        fillMarketListItems(defaultMarkets)
        // NumOffers are at default value (0)

        if (marketListItemsRequested) {
            job = coroutineScope.launch {
                try {
                    // TODO we might combine that api call to avoid 2 separate calls.
                    val markets = apiGateway.getMarkets()
                    fillMarketListItems(markets)
                    requestAndApplyNumOffers()
                    marketListItemsRequested = true
                } catch (e: Exception) {
                    log.e("Error at Fill Market List Items API request", e)
                }
            }
        }
        polling.start()
    }

    override fun deactivate() {
        cancelJob()
        polling.stop()
    }

    // Private
    private fun updateNumOffers() {
        cancelJob()
        job = coroutineScope.launch { requestAndApplyNumOffers() }
    }

    private fun fillMarketListItems(markets: List<Market>) {
        val list = markets.map { marketDto ->
            val market = Market(
                marketDto.baseCurrencyCode,
                marketDto.quoteCurrencyCode,
                marketDto.baseCurrencyName,
                marketDto.quoteCurrencyName,
            )

            MarketListItem(market)
        }
        _marketListItems.addAll(list)
    }

    private suspend fun requestAndApplyNumOffers() {
        try {
            val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()
            marketListItems.map { marketListItem ->
                val numOffers =
                    numOffersByMarketCode[marketListItem.market.quoteCurrencyCode] ?: 0
                marketListItem.setNumOffers(numOffers)
                marketListItem
            }
        } catch (e: Exception) {
            log.e("Error at apply num offers for markets API request", e)
        }
    }

    private fun cancelJob() {
        try {
            job?.cancel()
            job = null
        } catch (e: CancellationException) {
            log.e("Job cancel failed", e)
        }
    }
}