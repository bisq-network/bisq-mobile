package network.bisq.mobile.client.service.offers

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.service.offers.OfferFormattingUtil

class ClientOffersServiceFacade(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val apiGateway: OfferbookApiGateway,
    private val json: Json
) : OffersServiceFacade() {

    private var marketPriceUpdateJob: Job? = null

    // Misc
    private val offersMutex = Mutex()
    private var offerbookListItemsByMarket: MutableMap<String, MutableMap<String, OfferItemPresentationModel>> = mutableMapOf()
    private var offersSequenceNumber = atomic(-1)
    private var hasSubscribedToOffers = atomic(false)


    // Life cycle
    override fun activate() {
        super<OffersServiceFacade>.activate()

        observeMarketPrice()
        observeAvailableMarkets()
    }

    override fun deactivate() {
        _offerbookMarketItems.value = emptyList()
        hasSubscribedToOffers.value = false
        super<OffersServiceFacade>.deactivate()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        log.d { "Selecting offerbook market - Currency: ${marketListItem.market.quoteCurrencyCode}, Name: ${marketListItem.market.quoteCurrencyName}, NumOffers: ${marketListItem.numOffers}" }

        marketPriceServiceFacade.selectMarket(marketListItem)
        _selectedOfferbookMarket.value = OfferbookMarket(marketListItem.market)

        if (hasSubscribedToOffers.compareAndSet(expect = false, update = true)) {
            log.d { "First time subscribing to offers for market ${marketListItem.market.quoteCurrencyCode}" }
            subscribeOffers()
        } else {
            log.d { "Already subscribed to offers, applying filters for market ${marketListItem.market.quoteCurrencyCode}" }
            serviceScope.launch { applyOffersToSelectedMarket() }
        }
    }

    override suspend fun deleteOffer(offerId: String): Result<Boolean> {
        val result: Result<Unit> = apiGateway.deleteOffer(offerId)
        if (result.isSuccess) {
            return Result.success(true)
        } else {
            throw result.exceptionOrNull() ?: IllegalStateException("No Exception is set in result failure")
        }
    }

    override suspend fun createOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>
    ): Result<String> {
        val apiResult = apiGateway.publishOffer(
            direction, market, bitcoinPaymentMethods, fiatPaymentMethods, amountSpec, priceSpec, supportedLanguageCodes
        )
        if (apiResult.isSuccess) {
            return Result.success(apiResult.getOrThrow().offerId)
        } else {
            return Result.failure(apiResult.exceptionOrNull()!!)
        }
    }

    private fun observeAvailableMarkets() {
        launchIO {
            val result = apiGateway.getMarkets()
            if (result.isFailure) {
                result.exceptionOrNull()?.let { log.e { "GetMarkets request failed with exception $it" } }
                log.w { "GetMarkets failed, market list will remain empty" }
                return@launchIO
            }

            val markets = result.getOrThrow()
            fillMarketListItems(markets)
            subscribeNumOffers()
        }
    }

    private fun observeMarketPrice() {
        launchIO {
            runCatching {
                marketPriceServiceFacade.selectedMarketPriceItem.collectLatest { marketPriceItem ->
                    if (marketPriceItem != null) {
                        _selectedOfferbookMarket.value.setFormattedPrice(marketPriceItem.formattedPrice)
                        // Debounced per-offer updates when market price changes
                        scheduleOffersPriceRefresh()
                    }
                }
            }.onFailure {
                log.e(it) { "Error at marketPriceServiceFacade.selectedMarketPriceItem.collectLatest" }
            }
        }
    }

    private suspend fun subscribeNumOffers() {
        val observer = apiGateway.subscribeNumOffers()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }

            try {
                val webSocketEventPayload: WebSocketEventPayload<Map<String, Int>> = WebSocketEventPayload.from(
                    json,
                    webSocketEvent
                )
                val numOffersByMarketCode = webSocketEventPayload.payload

                _offerbookMarketItems.update { list ->
                    list.map { marketListItem ->
                        numOffersByMarketCode[marketListItem.market.quoteCurrencyCode]
                            ?.let { marketListItem.copy(numOffers = it) }
                                ?: marketListItem
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Error processing numOffers WebSocket event" }
            }
        }
    }

    private fun subscribeOffers() {
        serviceScope.launch {
            offersSequenceNumber.value = -1
            // We subscribe for all markets
            val observer = apiGateway.subscribeOffers()
            observer.webSocketEvent.collect { webSocketEvent ->
                if (webSocketEvent?.deferredPayload == null) {
                    return@collect
                }
                if (offersSequenceNumber.value >= webSocketEvent.sequenceNumber) {
                    log.w {
                        "Sequence number is larger or equal than the one we " + "received from the backend. We ignore that event."
                    }
                    return@collect
                }

                runCatching {
                    val webSocketEventPayload: WebSocketEventPayload<List<OfferItemPresentationDto>> = WebSocketEventPayload.from(
                        json,
                        webSocketEvent
                    )
                    val payload: List<OfferItemPresentationDto> = webSocketEventPayload.payload
                    log.d { "WebSocket offer update - Type: ${webSocketEvent.modificationType}, Count: ${payload.size}" }
                    updateOffersByMarket(webSocketEvent, payload)
                    applyOffersToSelectedMarket()
                }.onSuccess {
                    offersSequenceNumber.value = webSocketEvent.sequenceNumber
                }.onFailure { e ->
                    log.e(e) { "Error processing offers WebSocket event (seq=${webSocketEvent.sequenceNumber})" }
                }
            }
        }
    }

    private suspend fun updateOffersByMarket(
        webSocketEvent: WebSocketEvent,
        payload: List<OfferItemPresentationDto>
    ) {
        val modelsByMarket = payload.groupBy { it.bisqEasyOffer.market.quoteCurrencyCode }
            .mapValues { (_, items) ->
                items.associate { item ->
                    val model = OfferItemPresentationModel(item)
                    model.offerId to model
                }
            }

        offersMutex.withLock {
            when (webSocketEvent.modificationType) {
                ModificationType.REPLACE, ModificationType.ADDED -> {
                    modelsByMarket.forEach { (quoteCurrencyCode, models) ->
                        if (webSocketEvent.modificationType == ModificationType.REPLACE) {
                            // Clear only the specific market being replaced, not all markets
                            offerbookListItemsByMarket[quoteCurrencyCode]?.clear()
                        }

                        val marketMap = offerbookListItemsByMarket.getOrPut(quoteCurrencyCode) { mutableMapOf() }
                        marketMap.putAll(models)

                        models.keys.forEach { offerId ->
                            log.v { "${webSocketEvent.modificationType} offer $offerId for market $quoteCurrencyCode" }
                        }
                    }
                }
                ModificationType.REMOVED -> {
                    modelsByMarket.forEach { (quoteCurrencyCode, models) ->
                        offerbookListItemsByMarket[quoteCurrencyCode]?.let { map ->
                            models.keys.forEach { offerId ->
                                map.remove(offerId)
                                log.v { "REMOVED offer $offerId from market $quoteCurrencyCode" }
                            }
                            if (map.isEmpty()) {
                                offerbookListItemsByMarket.remove(quoteCurrencyCode)
                                log.d { "Removed empty market $quoteCurrencyCode from cache" }
                            }
                        }
                    }
                }
            }
        }
        log.d { "After ${webSocketEvent.modificationType} - Markets with offers: ${offerbookListItemsByMarket.mapValues { it.value.size }}" }
    }

    private suspend fun applyOffersToSelectedMarket() {
        val (selectedCurrency, availableMarkets, list) = offersMutex.withLock {
            val sc = selectedOfferbookMarket.value.market.quoteCurrencyCode
            val am = offerbookListItemsByMarket.keys.toList()
            val ofm = offerbookListItemsByMarket[sc]
            val l = ofm?.values?.toList()
            Triple(sc, am, l)
        }

        log.d { "Applying offers to selected market - Selected: $selectedCurrency" }
        log.d { "Available markets in cache: $availableMarkets" }
        log.d { "Offers found for $selectedCurrency: ${list?.size ?: 0}" }

        if (!list.isNullOrEmpty()) {
            log.d { "Offers for $selectedCurrency: ${list.map { "${'$'}{it.offerId} (${ '$' }{it.bisqEasyOffer.market.quoteCurrencyCode})" }}" }
        } else {
            log.w { "No offers found for selected market $selectedCurrency. Available markets: $availableMarkets" }
        }

        _offerbookListItems.value = list ?: emptyList()
    }

    private fun scheduleOffersPriceRefresh() {
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob = serviceScope.launch(Dispatchers.Default) {
            try {
                // Debounce to avoid UI churn during high-frequency price ticks
                delay(MARKET_TICK_DEBOUNCE_MS)
                refreshOffersFormattedValues()
            } catch (e: Exception) {
                log.e(e) { "Error scheduling offers price refresh (client)" }
            }
        }
    }

    private fun refreshOffersFormattedValues() {
        val marketItem = marketPriceServiceFacade.selectedMarketPriceItem.value ?: return
        val currentOffers = _offerbookListItems.value
        if (currentOffers.isEmpty()) return

        OfferFormattingUtil.updateOffersFormattedValues(currentOffers, marketItem)
    }

    private fun fillMarketListItems(markets: List<MarketVO>) {
        val marketListItems = markets.map { marketVO ->
            MarketListItem.from(marketVO)
        }

        _offerbookMarketItems.value = marketListItems
    }
}
