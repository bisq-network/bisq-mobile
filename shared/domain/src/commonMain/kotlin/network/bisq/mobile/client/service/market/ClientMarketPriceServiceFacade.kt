package network.bisq.mobile.client.service.market

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade

class ClientMarketPriceServiceFacade(
    private val apiGateway: MarketPriceApiGateway,
    private val json: kotlinx.serialization.json.Json,
    settingsRepository: SettingsRepository
) : MarketPriceServiceFacade(settingsRepository) {

    // Misc
    private val quotes: MutableMap<String, PriceQuoteVO> = mutableMapOf()
    private val quotesMutex = Mutex()
    private var selectedMarket: MarketVO? = null

    // Life cycle
    override fun activate() {
        super.activate()

        restoreSelectedMarketFromSettings {
            selectedMarket = it
            updateMarketPriceItem()
        }

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
                    log.d { "Client received price data for ${marketPriceMap.size} market price map markets: ${marketPriceMap.keys.take(10)}" }
                    quotesMutex.withLock {
                        quotes.putAll(marketPriceMap)
                    }
                    updateMarketPriceItem()
                    triggerGlobalPriceUpdate()
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

        persistSelectedMarketToSettings(marketListItem)
    }

    override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
        val quoteCurrencyCode: String = marketVO.quoteCurrencyCode
        return runBlocking {
            quotesMutex.withLock {
                quotes[quoteCurrencyCode]?.let { priceQuoteVO ->
                    val formattedPrice = MarketPriceFormatter.format(priceQuoteVO.value, marketVO)
                    MarketPriceItem(marketVO, priceQuoteVO, formattedPrice)
                }
            }
        }
    }

    override fun findUSDMarketPriceItem(): MarketPriceItem? {
        return findMarketPriceItem(MarketVOFactory.USD)
    }

    override fun refreshSelectedFormattedMarketPrice() {
        updateMarketPriceItem()
    }

    private fun updateMarketPriceItem() {
        selectedMarket?.let { market ->
            val quoteCurrencyCode: String = market.quoteCurrencyCode
            // Use runBlocking for synchronous access
            runBlocking {
                quotesMutex.withLock {
                    quotes[quoteCurrencyCode]?.let { priceQuote ->
                        val formattedPrice = MarketPriceFormatter.format(priceQuote.value, market)
                        val marketPriceItem = MarketPriceItem(market, priceQuote, formattedPrice)
                        _selectedMarketPriceItem.value = marketPriceItem
                        _selectedFormattedMarketPrice.value = formattedPrice
                        log.i { "upDateMarketPriceItem: code=$quoteCurrencyCode; priceQuote=$priceQuote; formattedPrice=$formattedPrice" }
                    }
                }
            }
        }
    }
}