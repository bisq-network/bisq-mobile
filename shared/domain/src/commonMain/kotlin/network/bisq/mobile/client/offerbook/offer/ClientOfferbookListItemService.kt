package network.bisq.mobile.client.offerbook.offer

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.utils.Logging

class ClientOfferbookListItemService(
    private val apiGateway: OfferbookApiGateway,
    private val json: Json
) :
    LifeCycleAware, Logging {


    // Properties
    private val _offerListItems = MutableStateFlow<List<OfferListItem>>(emptyList())
    val offerListItems: StateFlow<List<OfferListItem>> get() = _offerListItems

    // Misc
    private var job: Job? = null
    private var selectedMarket: MarketListItem? = null
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var sequenceNumber = atomic(-1)
    private var offerListItemsByMarket: MutableMap<String, MutableSet<OfferListItem>> =
        mutableMapOf()

    // Life cycle
    override fun activate() {
    }

    private fun subscribe() {
        if (job == null) {
            job = coroutineScope.launch {
                sequenceNumber = atomic(-1)
                // We subscribe for all markets
                val observer = apiGateway.subscribeOffers()
                observer?.webSocketEvent?.collect { webSocketEvent ->
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }
                    if (sequenceNumber.value >= webSocketEvent.sequenceNumber) {
                        log.w {
                            "Sequence number is larger or equal than the one we " +
                                    "received from the backend. We ignore that event."
                        }
                        return@collect
                    }

                    sequenceNumber.value = webSocketEvent.sequenceNumber
                    val webSocketEventPayload: WebSocketEventPayload<List<OfferListItem>> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    val payload: List<OfferListItem> = webSocketEventPayload.payload
                    if (webSocketEvent.modificationType == ModificationType.REPLACE ||
                        webSocketEvent.modificationType == ModificationType.ADDED
                    ) {
                        payload.forEach { item ->
                            offerListItemsByMarket.getOrPut(item.quoteCurrencyCode) { mutableSetOf() }
                                .add(item)
                        }
                    } else if (webSocketEvent.modificationType == ModificationType.REMOVED) {
                        payload.forEach { item ->
                            offerListItemsByMarket[item.quoteCurrencyCode]?.let { set ->
                                set.remove(item)
                                if (set.isEmpty()) {
                                    offerListItemsByMarket.remove(item.quoteCurrencyCode)
                                }
                            }
                        }
                    }
                    applyOffersToSelectedMarket()
                }
            }
        }
    }

    override fun deactivate() {
        cancelJob()
    }

    // API
    fun selectMarket(marketListItem: MarketListItem) {
        selectedMarket = marketListItem
        if (selectedMarket == null) {
            return
        }

        if (job == null) {
            subscribe()
        } else {
            applyOffersToSelectedMarket()
        }
    }

    private fun applyOffersToSelectedMarket() {
        _offerListItems.value =
            offerListItemsByMarket[getMarketCode()]?.toList()
                ?: emptyList()
    }

    private fun getMarketCode() = selectedMarket!!.market.quoteCurrencyCode

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}