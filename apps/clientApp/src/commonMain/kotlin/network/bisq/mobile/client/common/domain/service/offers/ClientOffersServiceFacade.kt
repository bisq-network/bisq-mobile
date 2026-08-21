package network.bisq.mobile.client.common.domain.service.offers

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OfferFormattingUtil
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import kotlin.concurrent.Volatile

class ClientOffersServiceFacade(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val apiGateway: OfferbookApiGateway,
    private val json: Json,
    private val webSocketClientService: WebSocketClientService,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val dispatcherProvider: DispatcherProvider,
) : OffersServiceFacade() {
    private companion object {
        // Upper bound for how long we show the blocking spinner while waiting for the initial
        // OFFERS snapshot. Aligned with the WebSocket request round-trip timeout (30s): on a cold
        // Tor connection the OFFERS snapshot can still take well over 10s to arrive. This is only
        // a spinner cap — the subscription is kept alive past it (see [startLoadingTimeout]), so a
        // late snapshot still populates.
        private const val LOADING_TIMEOUT_MS = 30000L
    }

    private var marketPriceUpdateJob: Job? = null

    private var loadingTimeoutJob: Job? = null

    private var ignoredIdsJob: Job? = null

    // Misc
    private val offersMutex = Mutex()
    private var offerbookListItemsByMarket: MutableMap<String, MutableMap<String, OfferItemPresentationModel>> = mutableMapOf()

    /**
     * Guards the single OFFERS WebSocket subscription for the facade lifetime. Set only after
     * [apiGateway.subscribeOffers] succeeds so market selection applies filters against the cache
     * instead of re-subscribing. Released on collector failure (see [resetOffersSubscriptionState])
     * or on [deactivate].
     */
    private var hasSubscribedToOffers = atomic(false)

    private val offersSubscriptionMutex = Mutex()

    /**
     * Coroutine handle for the active OFFERS subscription collector. Captured so it can be
     * cancelled cleanly on reset / deactivate without leaving orphan collectors attached
     * to stale observers.
     */
    private var offersSubscriptionJob: Job? = null

    private var getMarketsJob: Job? = null

    /** Latest NUM_OFFERS payload; replayed when [fillMarketListItems] runs after an early WS snapshot. */
    @Volatile
    private var cachedNumOffersByMarketCode: Map<String, Int>? = null

    // Life cycle
    override suspend fun activate() {
        super.activate()

        observeMarketPrice()
        observeAvailableMarkets()
        observeNumOffers()
        observeOffers()
        observeIgnoredProfiles()
    }

    override suspend fun deactivate() {
        _offerbookMarketItems.value = emptyList()
        cachedNumOffersByMarketCode = null
        offersSubscriptionJob?.cancel()
        offersSubscriptionJob = null
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob = null
        ignoredIdsJob?.cancel()
        ignoredIdsJob = null
        hasSubscribedToOffers.value = false
        super.deactivate()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem): Result<Unit> =
        runCatching<Unit> {
            log.d { "Selecting offerbook market - Currency: ${marketListItem.market.quoteCurrencyCode}, Name: ${marketListItem.market.quoteCurrencyName}, NumOffers: ${marketListItem.numOffers}" }

            marketPriceServiceFacade.selectMarket(marketListItem).getOrThrow()
            _selectedOfferbookMarket.value = OfferbookMarket(marketListItem.market)

            // If we don't have cached offers for the selected market yet, mark loading
            val code = marketListItem.market.quoteCurrencyCode
            val hasCache = offerbookListItemsByMarket[code]?.isNotEmpty() == true
            if (!hasCache) {
                _isOfferbookLoading.value = true
                startLoadingTimeout()
                prefetchOffersViaRest(code)
            } else {
                // We have cached offers, but if NUM_OFFERS reports a different count our cached
                // offers are stale — e.g. an offer the user just created (count already bumped)
                // whose OFFERS ADDED push hasn't arrived yet on a slow/Tor connection. Reconcile
                // via REST in the background (no spinner); the corrected list updates in place.
                val cachedCount = offerbookListItemsByMarket[code]?.size ?: 0
                val knownCount = cachedNumOffersByMarketCode?.get(code)
                if (knownCount != null && knownCount != cachedCount) {
                    log.d { "Cached offers for $code look stale (cached=$cachedCount, numOffers=$knownCount); reconciling via REST" }
                    prefetchOffersViaRest(code, replaceExisting = true)
                }
            }

            if (hasSubscribedToOffers.value) {
                log.d { "Already subscribed to offers, applying filters for market ${marketListItem.market.quoteCurrencyCode}" }
                serviceScope.launch {
                    try {
                        applyOffersToSelectedMarket()
                    } catch (t: Throwable) {
                        log.e(t) { "Error applying offers to selected market" }
                        _isOfferbookLoading.value = false
                        loadingTimeoutJob?.cancel()
                    }
                }
            } else {
                log.d { "Subscribing to offers for market ${marketListItem.market.quoteCurrencyCode}" }
                serviceScope.launch {
                    try {
                        startOffersSubscription("market select")
                        applyOffersToSelectedMarket()
                    } catch (t: Throwable) {
                        log.e(t) { "Error subscribing to offers for selected market" }
                        _isOfferbookLoading.value = false
                        loadingTimeoutJob?.cancel()
                    }
                }
            }
        }.onFailure { e ->
            log.e("Failed to select offerbook market: ${marketListItem.market}", e)
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
        market: network.bisq.mobile.data.replicated.common.currency.MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>,
    ): Result<String> {
        val apiResult =
            apiGateway.publishOffer(
                direction,
                market,
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                amountSpec,
                priceSpec,
                supportedLanguageCodes,
            )
        if (apiResult.isSuccess) {
            return Result.success(apiResult.getOrThrow().offerId)
        } else {
            return Result.failure(apiResult.exceptionOrNull()!!)
        }
    }

    private fun observeAvailableMarkets() {
        serviceScope.launch {
            offersMutex.withLock {
                getMarketsJob?.cancel()
                getMarketsJob =
                    // Through the provider rather than `Dispatchers.Default` directly: this job takes
                    // `offersMutex` (via fillMarketListItems -> tallyOffersByMarket) and shares it with
                    // the subscription collectors on `serviceScope`. A hard-wired real dispatcher puts
                    // the lock acquisition order on the wall clock, which no test can control — that is
                    // what made `ClientOffersServiceFacadeTest` flaky on loaded CI runners. Production
                    // is unchanged: `AppDispatcherProvider.default` is `Dispatchers.Default`.
                    serviceScope.launch(dispatcherProvider.default) {
                        webSocketClientService.connectionState.collect { state ->
                            if (state is ConnectionState.Connected) {
                                val result = apiGateway.getMarkets()
                                if (result.isFailure) {
                                    result
                                        .exceptionOrNull()
                                        ?.let { log.e { "GetMarkets request failed with exception $it" } }
                                    log.w { "GetMarkets failed, market list will remain empty" }
                                } else {
                                    val markets = result.getOrThrow()
                                    fillMarketListItems(markets)
                                    getMarketsJob?.cancel() // we only need this once
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun observeMarketPrice() {
        serviceScope.launch {
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

    private fun observeNumOffers() {
        serviceScope.launch {
            try {
                collectNumOffers(apiGateway.subscribeNumOffers())
            } catch (e: Exception) {
                log.e(e) { "Failed to subscribe to numOffers" }
            }
        }
    }

    /**
     * Registers the all-markets OFFERS subscription at activate so it joins the connect-time
     * subscription batch and the collector is ready before the first market is selected.
     * Launched instead of awaited: when the WS is already connected (lifecycle restart),
     * subscribing performs an immediate round-trip that can take ~30s over Tor and must not
     * block the serial facade-activation chain.
     */
    private fun observeOffers() {
        serviceScope.launch {
            try {
                startOffersSubscription("activate")
            } catch (e: Exception) {
                log.e(e) { "Failed to subscribe to offers at activate" }
            }
        }
    }

    private suspend fun startOffersSubscription(source: String) {
        offersSubscriptionMutex.withLock {
            if (hasSubscribedToOffers.value) {
                return
            }

            offersSubscriptionJob?.cancel()
            offersSubscriptionJob = null

            log.d { "Subscribing to offers at $source" }
            val observer = apiGateway.subscribeOffers()
            hasSubscribedToOffers.value = true

            offersSubscriptionJob =
                serviceScope.launch {
                    try {
                        collectOffers(observer)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.e(e) { "Offers subscription collector failed" }
                        resetOffersSubscriptionState()
                    }
                }
        }
    }

    private suspend fun collectOffers(observer: WebSocketEventObserver) {
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }

            runCatching {
                val webSocketEventPayload: WebSocketEventPayload<List<OfferItemPresentationDto>> =
                    WebSocketEventPayload.from(
                        json,
                        webSocketEvent,
                    )
                val payload: List<OfferItemPresentationDto> = webSocketEventPayload.payload
                log.d { "WebSocket offer update - Type: ${webSocketEvent.modificationType}, Count: ${payload.size}" }
                updateOffersByMarket(webSocketEvent, payload)
                applyOffersToSelectedMarket()
            }.onFailure { e ->
                // Log and skip: one malformed event must not tear down the subscription for all
                // markets. Genuine collector failures reset via the catch in
                // [startOffersSubscription] so the next market select can re-subscribe.
                log.e(e) { "Error processing offers WebSocket event (seq=${webSocketEvent.sequenceNumber}); skipping event" }
            }
        }
    }

    private suspend fun collectNumOffers(observer: WebSocketEventObserver) {
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }

            try {
                val webSocketEventPayload: WebSocketEventPayload<Map<String, Int>> =
                    WebSocketEventPayload.from(
                        json,
                        webSocketEvent,
                    )
                val numOffersByMarketCode = webSocketEventPayload.payload
                // Cached raw, as the node reported it: the count-aware loading check asks whether
                // the *node* considers the market empty, not whether it is empty for this device.
                cachedNumOffersByMarketCode = numOffersByMarketCode
                val talliesByMarket = tallyOffersByMarket()
                _offerbookMarketItems.update { list ->
                    applyNumOffersToMarketList(list, numOffersByMarketCode, talliesByMarket)
                }
            } catch (e: Exception) {
                log.e(e) { "Error processing numOffers WebSocket event" }
            }
        }
    }

    /**
     * Best-effort REST fast-path for the selected market. A direct REST request does not wait on
     * the OFFERS subscription snapshot and typically returns in a few seconds on slow connections.
     * This is purely additive: on failure we simply keep waiting for the subscription (unchanged
     * behaviour), and we never overwrite data the subscription has already delivered.
     */
    private fun prefetchOffersViaRest(
        code: String,
        replaceExisting: Boolean = false,
    ) {
        serviceScope.launch {
            runCatching { apiGateway.getOffers(code).getOrThrow() }
                .onSuccess { offers -> applyRestPrefetchedOffers(code, offers, replaceExisting) }
                .onFailure { e -> log.w(e) { "REST offers prefetch failed for $code; relying on the OFFERS subscription" } }
        }
    }

    private suspend fun applyRestPrefetchedOffers(
        code: String,
        offers: List<OfferItemPresentationDto>,
        replaceExisting: Boolean,
    ) {
        // Let the OFFERS subscription be the source of truth for genuinely-empty markets; only the
        // subscription snapshot (or its REMOVED events) can authoritatively confirm "no offers".
        if (offers.isEmpty()) return

        val models =
            offers.associate { dto ->
                val model = OfferItemPresentationModel(dto)
                model.offerId to model
            }

        var applied = false
        offersMutex.withLock {
            val marketMap = offerbookListItemsByMarket.getOrPut(code) { mutableMapOf() }
            when {
                // Reconcile: authoritative REST result replaces a stale cache (count mismatch).
                replaceExisting -> {
                    marketMap.clear()
                    marketMap.putAll(models)
                    applied = true
                }
                // Cold prefetch: only populate if the subscription hasn't delivered anything yet,
                // so we never clobber fresher subscription data.
                marketMap.isEmpty() -> {
                    marketMap.putAll(models)
                    applied = true
                }
            }
        }

        if (applied) {
            log.d { "REST prefetch (${if (replaceExisting) "reconcile" else "cold"}) populated ${models.size} offers for $code" }
            applyOffersToSelectedMarket()
        }
    }

    /**
     * Releases the offers subscription guard and cancels the collector. The next
     * [selectOfferbookMarket] call will trigger a fresh subscribe attempt.
     */
    private fun resetOffersSubscriptionState() {
        _isOfferbookLoading.value = false
        loadingTimeoutJob?.cancel()
        offersSubscriptionJob?.cancel()
        offersSubscriptionJob = null
        hasSubscribedToOffers.value = false
    }

    private suspend fun updateOffersByMarket(
        webSocketEvent: WebSocketEvent,
        payload: List<OfferItemPresentationDto>,
    ) {
        val modelsByMarket =
            payload
                .groupBy { it.bisqEasyOffer.market.quoteCurrencyCode }
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

    /**
     * Re-publishes the selected market whenever the ignore set changes, so ignoring a user hides
     * their offers straight away instead of leaving them on screen until some unrelated input
     * happens to refresh the list. Counterpart of `NodeOffersServiceFacade.observeIgnoredProfiles`.
     *
     * [applyOffersToSelectedMarket] republishes the counts too, exactly as the node's
     * `refreshCurrentChannelOffersAndCounts` does: the two are compared against each other by
     * [isSyncingSelectedMarketOffers], so letting them drift apart strands the UI in a sync spinner.
     */
    private fun observeIgnoredProfiles() {
        ignoredIdsJob?.cancel()
        ignoredIdsJob =
            serviceScope.launch {
                userProfileServiceFacade.ignoredProfileIds.collectLatest {
                    applyOffersToSelectedMarket()
                }
            }
    }

    /**
     * Publishes the selected market's offers, and the market counts along with them. Every path that
     * mutates [offerbookListItemsByMarket] ends here, so refreshing the counts from this one place
     * makes it impossible to fill the cache without correcting the counts that are measured against
     * it — a mismatch strands the offerbook behind [isSyncingSelectedMarketOffers] forever.
     */
    private suspend fun applyOffersToSelectedMarket() {
        // Offers, ignore set and cache tallies come from a single lock acquisition: taking them
        // separately lets an inbound offers event — or an ignore toggled from another screen — land
        // in between, publishing a list and a count that describe different snapshots.
        val (selectedCurrency, availableMarkets, list, ignoredProfileIds, talliesByMarket) =
            offersMutex.withLock {
                val sc = selectedOfferbookMarket.value.market.quoteCurrencyCode
                val am = offerbookListItemsByMarket.keys.toList()
                val ofm = offerbookListItemsByMarket[sc]
                val l = ofm?.values?.toList()
                val ignored = userProfileServiceFacade.ignoredProfileIds.value
                SelectedMarketSnapshot(sc, am, l, ignored, tallyOffersByMarketLocked(ignored))
            }

        log.d { "Applying offers to selected market - Selected: $selectedCurrency" }
        log.d { "Available markets in cache: $availableMarkets" }
        log.d { "Offers found for $selectedCurrency: ${list?.size ?: 0}" }

        if (!list.isNullOrEmpty()) {
            log.d { "Offers for $selectedCurrency: ${list.map { "${'$'}{it.offerId} (${ '$' }{it.bisqEasyOffer.market.quoteCurrencyCode})" }}" }
        } else {
            log.w { "No offers found for selected market $selectedCurrency. Available markets: $availableMarkets" }
        }

        // Counts before the list: at rest the two agree either way, but a reader that catches the
        // intermediate state should see a count that is briefly too low ("not syncing") rather than
        // a list that is briefly too long, which renders as the very spinner this ordering avoids.
        cachedNumOffersByMarketCode?.let { numOffersByMarketCode ->
            _offerbookMarketItems.update { marketItems ->
                applyNumOffersToMarketList(marketItems, numOffersByMarketCode, talliesByMarket)
            }
        }

        // Ignored makers' offers are not valid offerbook entries — same rule the node applies in
        // `isValidOfferbookMessage`, mirroring Bisq's `bisqEasyOfferbookMessageService.isValid`.
        // Applied on publish rather than on the way into `offerbookListItemsByMarket` so the cache
        // stays complete and un-ignoring restores the offers without waiting for a refetch.
        _offerbookListItems.value =
            list.orEmpty().filter { it.bisqEasyOffer.makerNetworkId.pubKey.id !in ignoredProfileIds }

        // Count-aware loading: NUM_OFFERS (from a separate, eagerly-subscribed topic) is the source
        // of truth for how many offers a market has. We only clear the spinner once the result is
        // authoritative: either the market has offers, or NUM_OFFERS explicitly reports zero for it.
        // A missing NUM_OFFERS entry means "not received yet" (unknown) — NOT zero — so in that case
        // we keep the spinner rather than flashing a false "no offers", and likewise while the count
        // says there are offers but the OFFERS snapshot slice hasn't caught up. The loading timeout
        // stays armed as a safety net for both.
        val knownNumOffers: Int? = cachedNumOffersByMarketCode?.get(selectedCurrency)
        // Deliberately the UNFILTERED list: NUM_OFFERS is counted by the node, which knows nothing
        // about who this device ignores. Judging arrival on the filtered list would keep the spinner
        // up forever in a market where every offer happens to come from an ignored maker.
        val hasOffers = !list.isNullOrEmpty()
        val confirmedEmpty = knownNumOffers == 0
        if (hasOffers || confirmedEmpty) {
            _isOfferbookLoading.value = false
            loadingTimeoutJob?.cancel()
        } else {
            log.d { "Keeping loading state for $selectedCurrency (knownNumOffers=$knownNumOffers, offers=${list?.size ?: 0})" }
        }
    }

    private fun scheduleOffersPriceRefresh() {
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob =
            serviceScope.launch(dispatcherProvider.default) {
                try {
                    // Debounce to avoid UI churn during high-frequency price ticks
                    delay(MARKET_TICK_DEBOUNCE_MS)
                    refreshOffersFormattedValues()
                } catch (e: CancellationException) {
                    // Every re-schedule cancels the pending job, so this fires on any burst of price
                    // ticks: that is the debounce working, not a failure. Swallowing it logged an
                    // error each time and left the job reported as completed rather than cancelled.
                    throw e
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

    private fun startLoadingTimeout() {
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob =
            serviceScope.launch {
                try {
                    delay(LOADING_TIMEOUT_MS)
                } catch (_: Exception) {
                    // job cancelled
                }
                if (_isOfferbookLoading.value) {
                    log.w { "Offerbook loading timed out for market ${selectedOfferbookMarket.value.market.quoteCurrencyCode}; keeping subscription alive for a late snapshot" }
                    // Only stop the blocking spinner. We deliberately KEEP the OFFERS subscription
                    // collector alive AND keep the subscription guard set:
                    //  - On a slow Tor cold start the OFFERS snapshot is queued behind the other
                    //    topics and can arrive well after this timeout; the alive collector then
                    //    populates the list reactively with no user action.
                    //  - We must NOT release the guard here: doing so lets a re-selection call
                    //    subscribeOffers() again, whose first act is to cancel the in-flight
                    //    subscription — throwing away the snapshot that is about to land. That churn
                    //    was exactly why offers only appeared after going back and forth.
                    // Recovery when the node genuinely never answers happens at the connection layer:
                    // a reconnect re-applies all registered subscriptions (incl. OFFERS) to the same
                    // observer, so the alive collector still receives the snapshot.
                    _isOfferbookLoading.value = false
                }
            }
    }

    private suspend fun fillMarketListItems(markets: List<network.bisq.mobile.data.replicated.common.currency.MarketVO>) {
        val numOffersByMarketCode = cachedNumOffersByMarketCode
        val talliesByMarket = tallyOffersByMarket()
        val marketListItems =
            markets.map { marketVO ->
                val advertised = numOffersByMarketCode?.get(marketVO.quoteCurrencyCode) ?: 0
                MarketListItem.from(marketVO, visibleNumOffers(marketVO.quoteCurrencyCode, advertised, talliesByMarket))
            }

        _offerbookMarketItems.value = marketListItems
    }

    private suspend fun tallyOffersByMarket(): Map<String, MarketOfferTally> =
        offersMutex.withLock {
            tallyOffersByMarketLocked(userProfileServiceFacade.ignoredProfileIds.value)
        }

    /**
     * Both tallies come from one walk of the cache, and only when something is ignored — with an
     * empty ignore set there is nothing to correct and every market keeps the node's raw count.
     *
     * Takes the ignore set rather than reading it: [applyOffersToSelectedMarket] filters its list
     * with the same value, and a second read there could see a different one.
     *
     * Caller must hold [offersMutex]; the mutex is not reentrant.
     */
    private fun tallyOffersByMarketLocked(ignoredProfileIds: Set<String>): Map<String, MarketOfferTally> {
        if (ignoredProfileIds.isEmpty()) return emptyMap()
        return offerbookListItemsByMarket.mapValues { (_, offersById) ->
            val ignored = offersById.values.count { it.bisqEasyOffer.makerNetworkId.pubKey.id in ignoredProfileIds }
            MarketOfferTally(ignored = ignored, visible = offersById.size - ignored)
        }
    }

    /** What one market's cached offers amount to for this device: hidden by an ignore, or shown. */
    private data class MarketOfferTally(
        val ignored: Int,
        val visible: Int,
    )

    private data class SelectedMarketSnapshot(
        val selectedCurrency: String,
        val availableMarkets: List<String>,
        val offers: List<OfferItemPresentationModel>?,
        val ignoredProfileIds: Set<String>,
        val talliesByMarket: Map<String, MarketOfferTally>,
    )

    /**
     * The node counts a market's offers without knowing who this device ignores, so subtract the
     * ignored ones we hold in cache. Mirrors `NumOffersObserver` on the node, which counts through
     * the very predicate that filters the node's list — leaving the two out of step here would make
     * [isSyncingSelectedMarketOffers] (`advertised > offers.size`) permanently true and spin forever.
     *
     * The floor is the other half of that: NUM_OFFERS and OFFERS are separate topics, so a maker's
     * offer being taken down can reach us as a lower node count *before* the delta drops it from the
     * cache. Subtracting a still-cached ignored offer from an already-reduced count would publish
     * fewer offers than the list on screen is showing.
     *
     * Markets whose offers have not been cached yet keep the raw count: there is nothing to subtract
     * and no way to know better until the snapshot lands.
     */
    private fun visibleNumOffers(
        marketCode: String,
        advertised: Int,
        talliesByMarket: Map<String, MarketOfferTally>,
    ): Int {
        val tally = talliesByMarket[marketCode] ?: return advertised
        return (advertised - tally.ignored).coerceAtLeast(tally.visible)
    }

    private fun applyNumOffersToMarketList(
        list: List<MarketListItem>,
        numOffersByMarketCode: Map<String, Int>,
        talliesByMarket: Map<String, MarketOfferTally>,
    ): List<MarketListItem> =
        list.map { marketListItem ->
            val marketCode = marketListItem.market.quoteCurrencyCode
            numOffersByMarketCode[marketCode]
                ?.let { marketListItem.copy(numOffers = visibleNumOffers(marketCode, it, talliesByMarket)) }
                ?: marketListItem
        }
}
