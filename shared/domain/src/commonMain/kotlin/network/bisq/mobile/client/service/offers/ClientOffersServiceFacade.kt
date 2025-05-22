package network.bisq.mobile.client.service.offers

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade

class ClientOffersServiceFacade(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val apiGateway: OfferbookApiGateway,
    private val json: Json
) : ServiceFacade(), OffersServiceFacade {

    // Properties
    private val _offerbookListItems = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    override val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> get() = _offerbookListItems

    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket //todo make nullable

    private val _offerbookMarketItems: MutableStateFlow<List<MarketListItem>> = MutableStateFlow(listOf())
    override val offerbookMarketItems: StateFlow<List<MarketListItem>> = _offerbookMarketItems

    // Misc
    private var offerbookListItemsByMarket: MutableMap<String, MutableMap<String, OfferItemPresentationModel>> = mutableMapOf()
    private var offersSequenceNumber = atomic(-1)
    private var hasSubscribedToOffers = atomic(false)


    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()

        observeMarketPrice()
        observeAvailableMarkets()
    }

    override fun deactivate() {
        _offerbookMarketItems.value = emptyList()
        hasSubscribedToOffers.value = false
        super<ServiceFacade>.deactivate()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        marketPriceServiceFacade.selectMarket(marketListItem)
        _selectedOfferbookMarket.value = OfferbookMarket(marketListItem.market)

        if (hasSubscribedToOffers.compareAndSet(expect = false, update = true)) {
            subscribeOffers()
        } else {
            applyOffersToSelectedMarket()
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
            return Result.failure(apiResult.exceptionOrNull()!!)
        }
    }

    private fun observeAvailableMarkets() {
        launchIO {
            val result = apiGateway.getMarkets()
            if (result.isFailure) {
                result.exceptionOrNull()
                    ?.let { log.e { "GetMarkets request failed with exception $it" } }
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

            val webSocketEventPayload: WebSocketEventPayload<Map<String, Int>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            val numOffersByMarketCode = webSocketEventPayload.payload
            offerbookMarketItems.value.map { marketListItem ->
                val numOffers = numOffersByMarketCode[marketListItem.market.quoteCurrencyCode] ?: 0
                marketListItem.setNumOffers(numOffers)
                marketListItem
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
                        val model = OfferItemPresentationModel(item)
                        val quoteCurrencyCode = item.bisqEasyOffer.market.quoteCurrencyCode
                        offerbookListItemsByMarket.getOrPut(quoteCurrencyCode) { mutableMapOf() }[model.offerId] = model
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

    private fun applyOffersToSelectedMarket() {
        val list = offerbookListItemsByMarket[selectedOfferbookMarket.value.market.quoteCurrencyCode]?.values?.toList()
        _offerbookListItems.value = list ?: emptyList()
    }

    private fun fillMarketListItems(markets: List<MarketVO>) {
        val marketListItems = markets.map { marketVO ->
            MarketListItem(marketVO)
        }

        _offerbookMarketItems.value = marketListItems
    }
}