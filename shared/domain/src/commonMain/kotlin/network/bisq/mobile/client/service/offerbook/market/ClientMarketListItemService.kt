package network.bisq.mobile.client.service.offerbook.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.utils.Logging


class ClientMarketListItemService(
    private val apiGateway: OfferbookApiGateway,
    private val json: Json
) : LifeCycleAware, Logging {


    // Properties
    private val _marketListItems: MutableList<MarketListItem> = mutableListOf()
    val marketListItems: List<MarketListItem> get() = _marketListItems

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null

    // Life cycle
    override fun activate() {
        cancelJob()
        job = coroutineScope.launch {
            val result = apiGateway.getMarkets()
            if (result.isFailure) {
                log.e { "GetMarkets request failed with exception ${result.exceptionOrNull()!!}" }
                return@launch
            }

            val markets = result.getOrThrow()
            fillMarketListItems(markets)
            val observer = apiGateway.subscribeNumOffers()
            observer?.webSocketEvent?.collect { webSocketEvent ->
                if (webSocketEvent?.deferredPayload == null) {
                    return@collect
                }

                val webSocketEventPayload: WebSocketEventPayload<Map<String, Int>> =
                    WebSocketEventPayload.from(json, webSocketEvent)
                val numOffersByMarketCode = webSocketEventPayload.payload
                marketListItems.map { marketListItem ->
                    val numOffers =
                        numOffersByMarketCode[marketListItem.market.quoteCurrencyCode] ?: 0
                    marketListItem.setNumOffers(numOffers)
                    marketListItem
                }
            }
        }
    }

    override fun deactivate() {
        cancelJob()
        _marketListItems.clear()
    }

    private fun fillMarketListItems(markets: List<MarketVO>) {
        val marketListItems = markets.map { marketVO ->
            MarketListItem(marketVO)
        }
        _marketListItems.clear()
        _marketListItems.addAll(marketListItems)
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}