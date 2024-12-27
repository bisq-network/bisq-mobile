package network.bisq.mobile.client.service.market

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.common.currency.Markets
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientMarketPriceServiceFacade(
    private val apiGateway: MarketPriceApiGateway,
    private val json: Json
) : MarketPriceServiceFacade, Logging {

    // Properties
    private val _selectedMarketPriceItem: MutableStateFlow<MarketPriceItem?> = MutableStateFlow(null)
    override val selectedMarketPriceItem: StateFlow<MarketPriceItem?> get() = _selectedMarketPriceItem

    private val _selectedFormattedMarketPrice = MutableStateFlow("N/A")
    override val selectedFormattedMarketPrice: StateFlow<String> = _selectedFormattedMarketPrice

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null
    private var selectedMarket: MarketVO = Markets.USD// todo use persisted or user default
    private val quotes = ConcurrentMap<String, PriceQuoteVO>()

    // Life cycle
    override fun activate() {
        job = coroutineScope.launch {
            val observer = apiGateway.subscribeMarketPrice()
            observer?.webSocketEvent?.collect { webSocketEvent ->
                try {
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }
                    val webSocketEventPayload: WebSocketEventPayload<Map<String, PriceQuoteVO>> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    val marketPriceMap = webSocketEventPayload.payload
                    quotes.putAll(marketPriceMap)
                    updateMarketPriceItem()
                } catch (e: Exception) {
                    log.e(e.toString(), e)
                }
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

    override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
        val quoteCurrencyCode: String = marketVO.quoteCurrencyCode
        return quotes[quoteCurrencyCode]?.let { priceQuoteVO ->
            val formattedPrice = MarketPriceFormatter.format(priceQuoteVO.value, marketVO)
            MarketPriceItem(marketVO, priceQuoteVO, formattedPrice)
        }
    }

    private fun updateMarketPriceItem() {
        val quoteCurrencyCode: String = selectedMarket.quoteCurrencyCode
        quotes[quoteCurrencyCode]?.let { priceQuote ->
            val formattedPrice = MarketPriceFormatter.format(priceQuote.value, selectedMarket)
            val marketPriceItem = MarketPriceItem(selectedMarket, priceQuote, formattedPrice)
            _selectedMarketPriceItem.value = marketPriceItem
            _selectedFormattedMarketPrice.value = formattedPrice
            log.i { "upDateMarketPriceItem: code=$quoteCurrencyCode; priceQuote =$priceQuote; formattedPrice =$formattedPrice" }
        }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}