package network.bisq.mobile.client.service.offers

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
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
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

class ClientOffersServiceFacade(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val apiGateway: OfferbookApiGateway,
    private val json: Json
) :
    OffersServiceFacade, Logging {

    // Properties
    private val _offerbookListItems = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    override val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> get() = _offerbookListItems

    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket //todo make nullable

    private val _offerbookMarketItems: MutableList<MarketListItem> = mutableListOf()
    override val offerbookMarketItems: List<MarketListItem> get() = _offerbookMarketItems

    // Misc
    private var offerbookListItemsByMarket: MutableMap<String, MutableMap<String, OfferItemPresentationModel>> = mutableMapOf()

    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var offersSequenceNumber = atomic(-1)
    private var subscribeOffersJob: Job? = null
    private var observeMarketPriceJob: Job? = null
    private var getMarketsJob: Job? = null


    // Life cycle
    override fun activate() {
        observeMarketPriceJob = observeMarketPrice()

        cancelGetMarketsJob()
        getMarketsJob = coroutineScope.launch {
            val result = apiGateway.getMarkets()
            if (result.isFailure) {
                log.e { "GetMarkets request failed with exception ${result.exceptionOrNull()!!}" }
                return@launch
            }

            val markets = result.getOrThrow()
            fillMarketListItems(markets)
            subscribeNumOffers()
        }
    }

    override fun deactivate() {
        cancelSubscribeOffersJob()
        cancelObserveMarketPriceJob()
        cancelGetMarketsJob()
        _offerbookMarketItems.clear()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        if (subscribeOffersJob == null) {
            subscribeOffers()
        } else {
            applyOffersToSelectedMarket()
        }

        marketPriceServiceFacade.selectMarket(marketListItem)

        _selectedOfferbookMarket.value = OfferbookMarket(marketListItem.market)

        cancelObserveMarketPriceJob()
        observeMarketPriceJob = observeMarketPrice()
    }

    override suspend fun deleteOffer(offerId: String): Result<Boolean> {
        val result: Result<Unit> = apiGateway.deleteOffer(offerId)
        if (result.isSuccess) {
            return Result.success(true)
        } else {
            throw result.exceptionOrNull()!!
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
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec,
            supportedLanguageCodes
        )
        if (apiResult.isSuccess) {
            return Result.success(apiResult.getOrThrow().offerId)
        } else {
            throw apiResult.exceptionOrNull()!!
        }
    }


    // Private
    private fun observeMarketPrice(): Job {
        return coroutineScope.launch {
            marketPriceServiceFacade.selectedMarketPriceItem.collectLatest { marketPriceItem ->
                if (marketPriceItem != null) {
                    _selectedOfferbookMarket.value.setFormattedPrice(marketPriceItem.formattedPrice)
                }
            }
        }
    }

    private suspend fun subscribeNumOffers() {
        val observer = apiGateway.subscribeNumOffers()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }

            val webSocketEventPayload: WebSocketEventPayload<Map<String, Int>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            val numOffersByMarketCode = webSocketEventPayload.payload
            offerbookMarketItems.map { marketListItem ->
                val numOffers =
                    numOffersByMarketCode[marketListItem.market.quoteCurrencyCode] ?: 0
                marketListItem.setNumOffers(numOffers)
                marketListItem
            }
        }
    }

    private fun subscribeOffers() {
        if (subscribeOffersJob == null) {
            subscribeOffersJob = coroutineScope.launch {
                offersSequenceNumber = atomic(-1)
                // We subscribe for all markets
                val observer = apiGateway.subscribeOffers()
                observer.webSocketEvent.collect { webSocketEvent ->
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }
                    if (offersSequenceNumber.value >= webSocketEvent.sequenceNumber) {
                        log.w {
                            "Sequence number is larger or equal than the one we " +
                                    "received from the backend. We ignore that event."
                        }
                        return@collect
                    }

                    offersSequenceNumber.value = webSocketEvent.sequenceNumber
                    val webSocketEventPayload: WebSocketEventPayload<List<OfferItemPresentationDto>> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    val payload: List<OfferItemPresentationDto> = webSocketEventPayload.payload
                    if (webSocketEvent.modificationType == ModificationType.REPLACE ||
                        webSocketEvent.modificationType == ModificationType.ADDED
                    ) {
                        payload.forEach { item ->
                            // TODO:
                            // to apply bisq.offer.price.spec.PriceSpecFormatter.getFormattedPriceSpec to item here.
                            val model = OfferItemPresentationModel(item)
                            offerbookListItemsByMarket.getOrPut(item.bisqEasyOffer.market.quoteCurrencyCode) { mutableMapOf() }
                                .put(model.offerId, model)
                        }
                    } else if (webSocketEvent.modificationType == ModificationType.REMOVED) {
                        payload.forEach { item ->
                            offerbookListItemsByMarket[item.bisqEasyOffer.market.quoteCurrencyCode]?.let { map ->
                                val model = OfferItemPresentationModel(item)
                                map.remove(model.offerId)
                                if (map.isEmpty()) {
                                    offerbookListItemsByMarket.remove(item.bisqEasyOffer.market.quoteCurrencyCode)
                                }
                            }
                        }
                    }
                    applyOffersToSelectedMarket()
                }
            }
        }
    }

    private fun applyOffersToSelectedMarket() {
        val list = offerbookListItemsByMarket[selectedOfferbookMarket.value.market.quoteCurrencyCode]?.values?.toList()
        _offerbookListItems.value = list ?: emptyList()
    }

    private fun cancelSubscribeOffersJob() {
        subscribeOffersJob?.cancel()
        subscribeOffersJob = null
    }

    private fun cancelObserveMarketPriceJob() {
        observeMarketPriceJob?.cancel()
        observeMarketPriceJob = null
    }

    private fun fillMarketListItems(markets: List<MarketVO>) {
        val marketListItems = markets.map { marketVO ->
            MarketListItem(marketVO)
        }
        _offerbookMarketItems.clear()
        _offerbookMarketItems.addAll(marketListItems)
    }

    private fun cancelGetMarketsJob() {
        getMarketsJob?.cancel()
        getMarketsJob = null
    }
}