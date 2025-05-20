package network.bisq.mobile.client.service.market

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade

class ClientMarketPriceServiceFacade(
    private val apiGateway: MarketPriceApiGateway,
    private val json: kotlinx.serialization.json.Json
) : ServiceFacade(), MarketPriceServiceFacade {

    // Properties
    private val _selectedMarketPriceItem: MutableStateFlow<MarketPriceItem?> = MutableStateFlow(null)
    override val selectedMarketPriceItem: StateFlow<MarketPriceItem?> get() = _selectedMarketPriceItem

    private val _selectedFormattedMarketPrice = MutableStateFlow("N/A")
    override val selectedFormattedMarketPrice: StateFlow<String> = _selectedFormattedMarketPrice

    // Misc
    private val quotes: MutableMap<String, PriceQuoteVO> = mutableMapOf()
    private var selectedMarket: MarketVO? = null

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()

        // Use the jobsManager to launch a coroutine instead of serviceScope directly
        launchIO {
            val observer = apiGateway.subscribeMarketPrice()
            observer.webSocketEvent.collect { webSocketEvent ->
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

    override fun findUSDMarketPriceItem(): MarketPriceItem? {
        return findMarketPriceItem(MarketVOFactory.USD)
    }

    override fun refreshSelectedFormattedMarketPrice() {
        updateMarketPriceItem()
    }

    private fun updateMarketPriceItem() {
        selectedMarket?.let {
            val quoteCurrencyCode: String = it.quoteCurrencyCode
            quotes[quoteCurrencyCode]?.let { priceQuote ->
                val formattedPrice = MarketPriceFormatter.format(priceQuote.value, it)
                val marketPriceItem = MarketPriceItem(it, priceQuote, formattedPrice)
                _selectedMarketPriceItem.value = marketPriceItem
                _selectedFormattedMarketPrice.value = formattedPrice
                log.i { "upDateMarketPriceItem: code=$quoteCurrencyCode; priceQuote =$priceQuote; formattedPrice =$formattedPrice" }
            }
        }
    }
}