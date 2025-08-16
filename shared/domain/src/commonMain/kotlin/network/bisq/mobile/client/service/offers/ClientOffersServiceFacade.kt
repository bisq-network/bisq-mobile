package network.bisq.mobile.client.service.offers

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
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
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils

class ClientOffersServiceFacade(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val userRepository: UserRepository,
    private val apiGateway: OfferbookApiGateway,
    private val json: Json
) : OffersServiceFacade() {

    // Misc
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
            applyOffersToSelectedMarket()
        }
    }

    override suspend fun deleteOffer(offerId: String): Result<Boolean> {
        val result: Result<Unit> = apiGateway.deleteOffer(offerId)
        if (result.isSuccess) {
            userRepository.updateLastActivity()
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
            userRepository.updateLastActivity()
            return Result.success(apiResult.getOrThrow().offerId)
        } else {
            return Result.failure(apiResult.exceptionOrNull()!!)
        }
    }

    override suspend fun createOfferWithMediatorWait(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>
    ): Result<String> {
        // For client mode, we delegate to the server which should handle mediator waiting
        return createOffer(
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec,
            supportedLanguageCodes
        )
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

                offersSequenceNumber.value = webSocketEvent.sequenceNumber
                val webSocketEventPayload: WebSocketEventPayload<List<OfferItemPresentationDto>> = WebSocketEventPayload.from(
                    json,
                    webSocketEvent
                )
                val payload: List<OfferItemPresentationDto> = webSocketEventPayload.payload
                log.d { "WebSocket offer update - Type: ${webSocketEvent.modificationType}, Count: ${payload.size}" }

                if (webSocketEvent.modificationType == ModificationType.REPLACE || webSocketEvent.modificationType == ModificationType.ADDED) {
                    payload.forEach { item ->
                        val model = OfferItemPresentationModel(item)
                        val quoteCurrencyCode = item.bisqEasyOffer.market.quoteCurrencyCode
                        offerbookListItemsByMarket.getOrPut(quoteCurrencyCode) { mutableMapOf() }[model.offerId] = model
                        log.v { "${webSocketEvent.modificationType} offer ${model.offerId} for market $quoteCurrencyCode" }
                    }
                    log.d { "After ${webSocketEvent.modificationType} - Markets with offers: ${offerbookListItemsByMarket.mapValues { it.value.size }}" }
                } else if (webSocketEvent.modificationType == ModificationType.REMOVED) {
                    payload.forEach { item ->
                        val quoteCurrencyCode = item.bisqEasyOffer.market.quoteCurrencyCode
                        offerbookListItemsByMarket[quoteCurrencyCode]?.let { map ->
                            val model = OfferItemPresentationModel(item)
                            map.remove(model.offerId)
                            log.v { "REMOVED offer ${model.offerId} from market $quoteCurrencyCode" }
                            if (map.isEmpty()) {
                                offerbookListItemsByMarket.remove(quoteCurrencyCode)
                                log.d { "Removed empty market $quoteCurrencyCode from cache" }
                            }
                        }
                    }
                    log.d { "After REMOVED - Markets with offers: ${offerbookListItemsByMarket.mapValues { it.value.size }}" }
                }
                applyOffersToSelectedMarket()
            }
        }
    }

    private fun applyOffersToSelectedMarket() {
        val selectedCurrency = selectedOfferbookMarket.value.market.quoteCurrencyCode
        val availableMarkets = offerbookListItemsByMarket.keys.toList()
        val offersForMarket = offerbookListItemsByMarket[selectedCurrency]
        val list = offersForMarket?.values?.toList()

        log.d { "Applying offers to selected market - Selected: $selectedCurrency" }
        log.d { "Available markets in cache: $availableMarkets" }
        log.d { "Offers found for $selectedCurrency: ${list?.size ?: 0}" }

        if (!list.isNullOrEmpty()) {
            log.d { "Offers for $selectedCurrency: ${list.map { "${it.offerId} (${it.bisqEasyOffer.market.quoteCurrencyCode})" }}" }
        } else {
            log.w { "No offers found for selected market $selectedCurrency. Available markets: $availableMarkets" }
        }

        _offerbookListItems.value = list ?: emptyList()
    }

    private fun fillMarketListItems(markets: List<MarketVO>) {
        val marketListItems = markets.map { marketVO ->
            MarketListItem(
                marketVO,
                0,
                CurrencyUtils.getLocaleFiatCurrencyName(
                    marketVO.quoteCurrencyCode,
                    marketVO.quoteCurrencyName
                )
            )
        }

        _offerbookMarketItems.value = marketListItems
    }
}
