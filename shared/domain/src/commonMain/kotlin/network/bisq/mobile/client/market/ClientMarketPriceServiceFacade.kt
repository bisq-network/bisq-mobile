package network.bisq.mobile.client.market

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.utils.Logging

class ClientMarketPriceServiceFacade(
    private val apiGateway: MarketPriceApiGateway,
    private val json: Json
) : MarketPriceServiceFacade, Logging {

    // Properties
    private val _selectedMarketPriceItem = MutableStateFlow(MarketPriceItem.EMPTY)
    override val selectedMarketPriceItem: StateFlow<MarketPriceItem> get() = _selectedMarketPriceItem

    private val _selectedFormattedMarketPrice = MutableStateFlow("N/A")
    override val selectedFormattedMarketPrice: StateFlow<String> = _selectedFormattedMarketPrice

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null
    private var selectedMarket: Market = Market.USD // todo use persisted or user default
    private val quotes = ConcurrentMap<String, Long>()

    // Life cycle
    override fun activate() {
        job = coroutineScope.launch {
            val observer = apiGateway.subscribeMarketPrice()
            observer?.webSocketEvent?.collect { webSocketEvent ->
                if (webSocketEvent?.deferredPayload == null) {
                    return@collect
                }
                val webSocketEventPayload: WebSocketEventPayload<Map<String, Long>> =
                    WebSocketEventPayload.from(json, webSocketEvent)
                val marketPriceMap = webSocketEventPayload.payload
                quotes.putAll(marketPriceMap)
                updateMarketPriceItem()
            }
        }
    }

    override fun deactivate() {
        cancelJob()
    }

    // API
    override fun selectMarket(marketListItem: MarketListItem) {
        selectedMarket = marketListItem.market
        updateMarketPriceItem()
    }

    private fun updateMarketPriceItem() {
        val quoteCurrencyCode: String = selectedMarket.quoteCurrencyCode
        quotes[quoteCurrencyCode]?.let { quote ->
            val formattedPrice = formatMarketPrice(selectedMarket, quote)
            val marketPriceItem = MarketPriceItem(selectedMarket, quote, formattedPrice)
            _selectedMarketPriceItem.value = marketPriceItem
            _selectedFormattedMarketPrice.value = formattedPrice
            log.i { "upDateMarketPriceItem: code=$quoteCurrencyCode; quote =$quote; formattedPrice =$formattedPrice" }
        }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}