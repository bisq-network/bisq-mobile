package network.bisq.mobile.client.offerbook.offer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.service.Polling
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.offerbook.OfferListItem
import network.bisq.mobile.domain.offerbook.market.MarketListItem
import network.bisq.mobile.utils.Logging


class ClientOfferbookListItemService(private val apiGateway: OfferbookApiGateway) :
    LifeCycleAware, Logging {


    // Properties
    private val _offerListItems = MutableStateFlow<List<OfferListItem>>(emptyList())
    val offerListItems: StateFlow<List<OfferListItem>> get() = _offerListItems

    // Misc
    private var getOffersJob: Job? = null
    private var polling = Polling(1000) { updateOffers() }
    private var selectedMarket: MarketListItem? = null

    // Life cycle
    override fun activate() {
        polling.start()
    }

    override fun deactivate() {
        getOffersJob?.cancel()
        getOffersJob = null
        polling.stop()
    }

    // API
    fun selectMarket(marketListItem: MarketListItem) {
        selectedMarket = marketListItem
        updateOffers()
    }

  private  fun updateOffers() {
        if (selectedMarket != null) {
            getOffersJob?.cancel()
            getOffersJob = CoroutineScope(BackgroundDispatcher).launch {
                try {
                    if (selectedMarket != null) {
                        _offerListItems.value =
                            apiGateway.getOffers(selectedMarket!!.market.quoteCurrencyCode)
                    }
                } catch (e: Exception) {
                    log.e("Error at getOffers API request", e)
                }
            }
        }
    }
}