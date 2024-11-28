package network.bisq.mobile.client.market

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.service.Polling
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.utils.Logging

class ClientMarketPriceServiceFacade(
    private val apiGateway: MarketPriceApiGateway
) : MarketPriceServiceFacade, Logging {

    // Properties
    private val _marketPriceItem = MutableStateFlow(MarketPriceItem(Market.USD))
    override val marketPriceItem: StateFlow<MarketPriceItem> get() = _marketPriceItem

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null
    private var polling = Polling(60000) { requestMarketPriceQuotes() }
    private var selectedMarket: Market = Market.USD // todo use persisted or user default
    private val quotes: HashMap<String, Long> = HashMap()

    // Life cycle
    override fun activate() {
        requestMarketPriceQuotes()
        polling.start()
    }

    override fun deactivate() {
        cancelJob()
        polling.stop()
    }

    // API
    override fun selectMarket(marketListItem: MarketListItem) {
        selectedMarket = marketListItem.market
        _marketPriceItem.value = MarketPriceItem(marketListItem.market)
        applyQuote()
    }

    // Private
    private fun requestMarketPriceQuotes() {
        selectedMarket.let {
            cancelJob()
            job = coroutineScope.launch {
                try {
                    val response: MarketPriceResponse = apiGateway.getQuotes()
                    quotes.putAll(response.quotes)

                    applyQuote()
                } catch (e: Exception) {
                    log.e("Error at API request", e)
                }
            }
        }
    }

    private fun applyQuote() {
        val code = _marketPriceItem.value.market.quoteCurrencyCode
        quotes[code]?.let {
            _marketPriceItem.value.setQuote(it)
            log.i { "applyQuote: code=$code; quote =$it" }
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