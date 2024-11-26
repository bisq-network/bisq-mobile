package network.bisq.mobile.client.offerbook.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        // TODO we should fill it with static default data as the list is only changing if we add a
        //  new currency to the price feed service. With that we have the list immediately and we
        //  can do a request to cover the case that markets have been added.
        if (!marketListItemsRequested) {
            CoroutineScope(BackgroundDispatcher).launch {
                try {
                    val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()
                    marketListItemsRequested = true
                    val list = apiGateway.getMarkets()
                        .map { marketDto ->
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
    private fun updateNumOffers() {
        CoroutineScope(BackgroundDispatcher).launch {
            try {
                val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()
                marketListItems.map { marketListItem ->
                    val numOffers = numOffersByMarketCode[marketListItem.market.quoteCurrencyCode] ?: 0
                    marketListItem.setNumOffers(numOffers)
                    marketListItem
                }
            } catch (e: Exception) {
                log.e("Error at API request", e)
            }
        }
    }
}