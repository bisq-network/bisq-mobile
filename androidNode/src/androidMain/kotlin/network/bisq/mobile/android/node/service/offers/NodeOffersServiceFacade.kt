package network.bisq.mobile.android.node.service.offers

import bisq.account.payment_method.BitcoinPaymentMethod
import bisq.account.payment_method.BitcoinPaymentMethodUtil
import bisq.account.payment_method.FiatPaymentMethod
import bisq.account.payment_method.FiatPaymentMethodUtil
import bisq.bisq_easy.BisqEasyServiceUtil
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.currency.Market
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.network.p2p.services.data.BroadcastResult
import bisq.offer.Direction
import bisq.offer.amount.spec.AmountSpec
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.price.spec.PriceSpec
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.mapping.OfferItemPresentationVOFactory
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
import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import java.util.Date
import java.util.Optional


class NodeOffersServiceFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userRepository: UserRepository
) : OffersServiceFacade() {

    companion object {
        private const val SMALL_DELAY = 25L
        private const val SMALL_DELAY_THRESHOLD = 5
        // Higher threshold to avoid masking memory leaks - only suggest GC in critical situations
        private const val MEMORY_GC_THRESHOLD = 0.85
        private const val OFFER_BATCH_DELAY = 100L // milliseconds
        private const val MAP_CLEAR_THRESHOLD = 50
        private const val MIN_GC_INTERVAL = 10000L
        private val MEDIATOR_WAIT_TIMEOUT = 1.minutes
        private val MEDIATOR_POLL_INTERVAL = 2.seconds
        private val MEMORY_LOG_INTERVAL = 30.seconds
    }

    // Dependencies
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val reputationService: ReputationService by lazy { applicationService.userService.get().reputationService }
    private val bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelSelectionService }

//  TODO restore for usage of v2.1.8
//    private val bisqEasyOfferbookMessageService: BisqEasyOfferbookMessageService by lazy { applicationService.bisqEasyService.get().bisqEasyOfferbookMessageService }


    // Misc
    private var selectedChannel: BisqEasyOfferbookChannel? = null

    private val bisqEasyOfferbookMessageByOfferId: MutableMap<String, BisqEasyOfferbookMessage> = mutableMapOf()
    private val offerMapMutex = Mutex()
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null
    private var marketPricePin: Pin? = null
    private var memoryMonitoringJob: kotlinx.coroutines.Job? = null
    private var offerBatchJob: kotlinx.coroutines.Job? = null
    private val pendingOffers = ConcurrentLinkedQueue<BisqEasyOfferbookMessage>()
    private val batchMutex = Mutex()

    private var lastGcTime = 0L

    // Life cycle
    override fun activate() {
        log.d { "Activating NodeOffersServiceFacade" }
        super.activate()

        observeSelectedChannel()
        observeMarketPrice()
        if (numOffersObservers.isNotEmpty())  {
            numOffersObservers.forEach { it.resume() }
        } else {
            observeMarketListItems(_offerbookMarketItems)
        }
        startMemoryMonitoring()
        log.d { "NodeOffersServiceFacade activated, numOffersObservers: ${numOffersObservers.size}" }
    }

    override fun deactivate() {
        log.d { "Deactivating NodeOffersServiceFacade" }
        pendingOffers.clear()
        chatMessagesPin?.unbind()
        chatMessagesPin = null
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null
        numOffersObservers.forEach { it.dispose() }
        log.d { "NodeOffersServiceFacade deactivated" }

        super.deactivate()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        log.d { "Selecting offerbook market: ${marketListItem.market.quoteCurrencyCode}, current offers count: ${_offerbookListItems.value.size}" }
        val market = Mappings.MarketMapping.toBisq2Model(marketListItem.market)
        val channelOptional = bisqEasyOfferbookChannelService.findChannel(market)
        
        if (!channelOptional.isPresent) {
            log.e { "No channel found for market ${market.marketCodes}" }
            return
        }
        
        val channel = channelOptional.get()
        log.d { "Found channel for market ${market.marketCodes}, chat messages count: ${channel.chatMessages.size}" }
        
        bisqEasyOfferbookChannelSelectionService.selectChannel(channel)
        marketPriceServiceFacade.selectMarket(marketListItem)
    }

    override suspend fun deleteOffer(offerId: String): Result<Boolean> {
        try {
            val optionalOfferbookMessage = findBisqEasyOfferbookMessage(offerId)
            check(optionalOfferbookMessage.isPresent) { "BisqEasyOfferbookMessage with offer ID $offerId not found" }
            val offerbookMessage = optionalOfferbookMessage.get()
            val authorUserProfileId: String = offerbookMessage.authorUserProfileId
            val optionalUserIdentity = userIdentityService.findUserIdentity(authorUserProfileId)
            check(optionalUserIdentity.isPresent) { "UserIdentity for authorUserProfileId $authorUserProfileId not found" }
            val userIdentity = optionalUserIdentity.get()
            check(userIdentity == userIdentityService.selectedUserIdentity) { "Selected selectedUserIdentity does not match the offers authorUserIdentity" }
            val broadcastResult: BroadcastResult =
                bisqEasyOfferbookChannelService.deleteChatMessage(offerbookMessage, userIdentity.networkIdWithKeyPair).join()
            val broadcastResultNotEmpty = broadcastResult.isNotEmpty()
            if (!broadcastResultNotEmpty) {
                log.w { "Delete offer message was not broadcast to network. Maybe there are no peers connected." }
            }
            userRepository.updateLastActivity()
            return Result.success(broadcastResultNotEmpty)
        } catch (e: Exception) {
            return Result.failure(e)
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
        return try {
            val offerId = createOffer(
                Mappings.DirectionMapping.toBisq2Model(direction),
                Mappings.MarketMapping.toBisq2Model(market),
                bitcoinPaymentMethods.map { BitcoinPaymentMethodUtil.getPaymentMethod(it) },
                fiatPaymentMethods.map { FiatPaymentMethodUtil.getPaymentMethod(it) },
                Mappings.AmountSpecMapping.toBisq2Model(amountSpec),
                Mappings.PriceSpecMapping.toBisq2Model(priceSpec),
                ArrayList<String>(supportedLanguageCodes)
            )
            userRepository.updateLastActivity()
            Result.success(offerId)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer: ${e.message}" }
            Result.failure(e)
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
        return try {
            val offerId = createOfferWithMediatorWait(
                Mappings.DirectionMapping.toBisq2Model(direction),
                Mappings.MarketMapping.toBisq2Model(market),
                bitcoinPaymentMethods.map { BitcoinPaymentMethodUtil.getPaymentMethod(it) },
                fiatPaymentMethods.map { FiatPaymentMethodUtil.getPaymentMethod(it) },
                Mappings.AmountSpecMapping.toBisq2Model(amountSpec),
                Mappings.PriceSpecMapping.toBisq2Model(priceSpec),
                ArrayList<String>(supportedLanguageCodes)
            )
            userRepository.updateLastActivity()
            Result.success(offerId)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer with mediator wait: ${e.message}" }
            Result.failure(e)
        }
    }

    // Private
    private fun createOffer(
        direction: Direction,
        market: Market,
        bitcoinPaymentMethods: List<BitcoinPaymentMethod>,
        fiatPaymentMethods: List<FiatPaymentMethod>,
        amountSpec: AmountSpec,
        priceSpec: PriceSpec,
        supportedLanguageCodes: List<String>
    ): String {
        val userIdentity: UserIdentity = userIdentityService.selectedUserIdentity
        val chatMessageText = BisqEasyServiceUtil.createOfferBookMessageFromPeerPerspective(
            userIdentity.nickName,
            marketPriceService,
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec
        )
        val userProfile = userIdentity.userProfile
        val bisqEasyOffer = BisqEasyOffer(
            userProfile.networkId,
            direction,
            market,
            amountSpec,
            priceSpec,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            userProfile.terms,
            supportedLanguageCodes,
//            TODO for Bisq v2.1.8
//            BuildNodeConfig.TRADE_PROTOCOL_VERSION,
        )



        val channel: BisqEasyOfferbookChannel = bisqEasyOfferbookChannelService.findChannel(market).get()

        val myOfferMessage = BisqEasyOfferbookMessage(
            channel.id,
            userProfile.id,
            Optional.of(bisqEasyOffer),
            Optional.of(chatMessageText),
            Optional.empty(),
            Date().time,
            false
        )

        // blocking call
        bisqEasyOfferbookChannelService.publishChatMessage(myOfferMessage, userIdentity).join()
        return bisqEasyOffer.id
    }

    private suspend fun createOfferWithMediatorWait(
        direction: Direction,
        market: Market,
        bitcoinPaymentMethods: List<BitcoinPaymentMethod>,
        fiatPaymentMethods: List<FiatPaymentMethod>,
        amountSpec: AmountSpec,
        priceSpec: PriceSpec,
        supportedLanguageCodes: List<String>
    ): String {
        val mediationRequestService = applicationService.supportService.get().mediationRequestService
        val userIdentity: UserIdentity = userIdentityService.selectedUserIdentity

        log.d { "Checking mediator availability..." }

        try {
            return withTimeout(MEDIATOR_WAIT_TIMEOUT) {
                // Check immediately, then poll with delay
                var firstCheck = true
                while (true) {
                    if (!firstCheck) {
                        delay(MEDIATOR_POLL_INTERVAL)
                    }

                    val currentMediator = mediationRequestService.selectMediator(
                        userIdentity.userProfile.id,
                        userIdentity.userProfile.id,
                        "temp-offer-id"
                    )

                    if (currentMediator.isPresent) {
                        log.d { "Mediator available, creating offer" }
                        return@withTimeout createOffer(direction, market, bitcoinPaymentMethods, fiatPaymentMethods, amountSpec, priceSpec, supportedLanguageCodes)
                    }

                    if (firstCheck) {
                        log.d { "No mediator available, waiting up to ${MEDIATOR_WAIT_TIMEOUT.inWholeSeconds} seconds..." }
                    }

                    firstCheck = false
                }
                @Suppress("UNREACHABLE_CODE")
                error("Unreachable")
            }
        } catch (e: TimeoutCancellationException) {
            throw MediatorNotAvailableException("Timeout waiting for mediator after ${MEDIATOR_WAIT_TIMEOUT.inWholeSeconds} seconds")
        }
    }

    private fun observeSelectedChannel() {
        log.d { "Setting up selected channel observer" }
        selectedChannelPin?.unbind()
        log.d { "Previous selectedChannelPin unbound: $selectedChannelPin" }
        
        selectedChannelPin = bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
            if (channel == null) {
                log.d { "Selected channel is null" }
                selectedChannel = channel
                log.d { "After null channel selection, offers count: ${_offerbookListItems.value.size}" }
            } else if (channel is BisqEasyOfferbookChannel) {
                log.d { "Selected channel changed to: ${channel.id}, market: ${channel.market.marketCodes}, chat messages: ${channel.chatMessages.size}" }
                selectedChannel = channel
                marketPriceService.setSelectedMarket(channel.market)
                val marketVO = Mappings.MarketMapping.fromBisq2Model(channel.market)
                _selectedOfferbookMarket.value = OfferbookMarket(marketVO)
                updateMarketPrice()

                // Clear the map synchronously before adding observers
                launchIO {
                    clearOfferMessages()
                    addChatMessagesObservers(channel)
                }
                
                log.d { "After channel selection, offers count: ${_offerbookListItems.value.size}" }
            } else {
                log.w { "Selected channel is not a BisqEasyOfferbookChannel: ${channel::class.simpleName}" }
            }
        }
        log.d { "Selected channel observer set up, pin: $selectedChannelPin" }
    }

    private fun createOfferListItem(bisqEasyOfferbookMessage: BisqEasyOfferbookMessage): OfferItemPresentationDto {
        return OfferItemPresentationVOFactory.create(
            userProfileService,
            userIdentityService,
            marketPriceService,
            reputationService,
            bisqEasyOfferbookMessage
        )
    }

    private fun addChatMessagesObservers(channel: BisqEasyOfferbookChannel) {
        log.d { "Adding chat message observers for channel: ${channel.id}, market: ${channel.market.marketCodes}" }
        chatMessagesPin?.unbind()
        log.d { "Previous chatMessagesPin unbound" }
        
        // Only clear the list, not the map (map is cleared before this method is called)
        _offerbookListItems.value = emptyList()

        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = channel.chatMessages
        log.d { "Initial chat messages count for ${channel.market.marketCodes}: ${chatMessages.size}" }
        
        if (chatMessages.isEmpty()) {
            log.w { "Channel ${channel.market.marketCodes} has no chat messages/offers" }
        }
        
        chatMessagesPin =
            chatMessages.addObserver(object : CollectionObserver<BisqEasyOfferbookMessage> {
                override fun add(message: BisqEasyOfferbookMessage) {
                    if (!message.bisqEasyOffer.isPresent) {
                        return
                    }

                    // Add to thread-safe queue (non-blocking)
                    pendingOffers.offer(message)
                    startOffersBatchJob()
                }

                override fun remove(message: Any) {
                    if (message is BisqEasyOfferbookMessage && message.bisqEasyOffer.isPresent) {
                        val offerId = message.bisqEasyOffer.get().id
                        log.d { "Removing offer message: $offerId" }
                        val item = _offerbookListItems.value.firstOrNull {
                            it.bisqEasyOffer.id == message.bisqEasyOffer.map { offer -> offer.id }.orElse(null)
                        }
                        item?.let { model ->
                            _offerbookListItems.update { it - model }
                            launchIO { removeOfferMessage(offerId) }
                            log.i { "Removed offer: $offerId, remaining offers: ${_offerbookListItems.value.size}" }
                        }
                    }
                }

                override fun clear() {
                    log.d { "Clearing all offer messages" }
                    _offerbookListItems.value = emptyList()
                    launchIO { clearOfferMessages() }
                }
            })
        
        log.d { "Chat messages observer added for ${channel.market.marketCodes}, pin: $chatMessagesPin" }
    }

    private fun startOffersBatchJob() {
        // Use mutex to prevent race conditions when starting batch jobs
        launchIO {
            batchMutex.withLock {
                // Only start a new job if none is running
                if (offerBatchJob?.isActive != true) {
                    offerBatchJob = serviceScope.launch(Dispatchers.Default) {
                        delay(OFFER_BATCH_DELAY)
                        processPendingOffers()
                    }
                }
            }
        }
    }

    private fun observeMarketListItems(itemsFlow: MutableStateFlow<List<MarketListItem>>) {
        log.d { "Observing market list items" }
        numOffersObservers.forEach { it.dispose() }
        numOffersObservers.clear()

        val channels = bisqEasyOfferbookChannelService.channels
        val initialItems = channels.map { channel ->
            val marketVO = MarketVO(
                channel.market.baseCurrencyCode,
                channel.market.quoteCurrencyCode,
                channel.market.baseCurrencyName,
                channel.market.quoteCurrencyName,
            )
            MarketListItem(marketVO, channel.chatMessages.size)
        }
        itemsFlow.value = initialItems

        channels.forEach { channel ->
            val marketVO = MarketVO(
                channel.market.baseCurrencyCode,
                channel.market.quoteCurrencyCode,
                channel.market.baseCurrencyName,
                channel.market.quoteCurrencyName,
            )
            val market = Mappings.MarketMapping.toBisq2Model(marketVO)
            if (marketPriceService.marketPriceByCurrencyMap.isEmpty() ||
                marketPriceService.marketPriceByCurrencyMap.containsKey(market)
            ) {
                val numOffersObserver = NumOffersObserver(
                    channel,
                    { numOffers ->
                        val safeNumOffers = numOffers ?: 0
                        // Rebuild the list immutably
                        itemsFlow.value = itemsFlow.value.map {
                            if (it.market == marketVO) it.copy(numOffers = safeNumOffers) else it
                        }
                    },
                )
                numOffersObservers.add(numOffersObserver)
                log.d { "Added market ${market.marketCodes} with initial offers count: ${channel.chatMessages.size}" }
            } else {
                log.d { "Skipped market ${market.marketCodes} - not in marketPriceByCurrencyMap" }
            }
        }
        log.d { "Filled market list items, count: ${itemsFlow.value.size}" }
    }

    private fun observeMarketPrice() {
        marketPricePin = marketPriceService.marketPriceByCurrencyMap.addObserver(Runnable {
            marketPriceService.findMarketPriceQuote(marketPriceService.selectedMarket.get())
            updateMarketPrice()
        })
    }

    private fun updateMarketPrice() {
        if (marketPriceServiceFacade.selectedMarketPriceItem.value != null) {
            val formattedPrice = marketPriceServiceFacade.selectedMarketPriceItem.value!!.formattedPrice
            _selectedOfferbookMarket.value.setFormattedPrice(formattedPrice)
        }
    }

    private fun isValidOfferMessage(message: BisqEasyOfferbookMessage): Boolean {
//    TODO restore for usage of core version v2.1.8
//        return bisqEasyOfferbookMessageService.isValid(message)
        // Basic validation - message must have an offer
        if (!message.hasBisqEasyOffer()) {
            return false
        }

        val offer = message.bisqEasyOffer.get()

        // Check if the maker's user profile exists and is not banned/ignored
        val makerUserProfile = userProfileService.findUserProfile(offer.makersUserProfileId)
        if (makerUserProfile.isEmpty) {
            return false
        }

        if (userProfileService.isChatUserIgnored(makerUserProfile.get())) {
            return false
        }

        // Don't show our own offers
//        val myUserIdentityIds = userIdentityService.userIdentities.map { it.userProfile.id }.toSet()
//        if (myUserIdentityIds.contains(makerUserProfile.get().id)) {
//            return false
//        }

        return true
    }

    private suspend fun findBisqEasyOfferbookMessage(offerId: String): Optional<BisqEasyOfferbookMessage> {
        return Optional.ofNullable(getOfferMessage(offerId))
    }

    private suspend fun putOfferMessage(offerId: String, message: BisqEasyOfferbookMessage) {
        offerMapMutex.withLock {
            bisqEasyOfferbookMessageByOfferId[offerId] = message
        }
    }

    private suspend fun removeOfferMessage(offerId: String) {
        offerMapMutex.withLock {
            bisqEasyOfferbookMessageByOfferId.remove(offerId)
        }
    }

    private suspend fun clearOfferMessages() {
        offerMapMutex.withLock {
            val currentSize = bisqEasyOfferbookMessageByOfferId.size
            log.d { "Clearing offer messages map, current size: $currentSize" }
            bisqEasyOfferbookMessageByOfferId.clear()

            // Suggest GC after clearing large collections
            if (currentSize > MAP_CLEAR_THRESHOLD) {
                log.w { "MEMORY: Cleared large offer map ($currentSize items), suggesting GC" }
                suggestGCtoOS()
            }
        }
    }

    private suspend fun getOfferMessage(offerId: String): BisqEasyOfferbookMessage? {
        return offerMapMutex.withLock {
            bisqEasyOfferbookMessageByOfferId[offerId]
        }
    }

    private suspend fun offerMessagesContainsKey(offerId: String): Boolean {
        return offerMapMutex.withLock {
            val contains = bisqEasyOfferbookMessageByOfferId.containsKey(offerId)
            log.d { "Checking if offer $offerId exists in map: $contains, map size: ${bisqEasyOfferbookMessageByOfferId.size}" }
            contains
        }
    }

    private suspend fun processPendingOffers() {
        // Drain the thread-safe queue (non-blocking)
        val offersToProcess = mutableListOf<BisqEasyOfferbookMessage>()
        while (true) {
            val offer = pendingOffers.poll() ?: break
            offersToProcess.add(offer)
        }

        if (offersToProcess.isEmpty()) return

        val newOffers = mutableListOf<OfferItemPresentationModel>()
        var processedCount = 0

        // Process in smaller chunks to reduce memory pressure
        offersToProcess.chunked(5).forEach { chunk ->
            for (message in chunk) {
                try {
                    val offerId = message.bisqEasyOffer.get().id

                    // Quick validation before expensive operations
                    if (offerMessagesContainsKey(offerId) || !isValidOfferMessage(message)) {
                        continue
                    }

                    // Create objects only after validation
                    val offerItemPresentationDto: OfferItemPresentationDto = createOfferListItem(message)
                    val offerItemPresentationModel = OfferItemPresentationModel(offerItemPresentationDto)

                    newOffers.add(offerItemPresentationModel)
                    putOfferMessage(offerId, message)
                    processedCount++

                } catch (e: Exception) {
                    log.e(e) { "Error processing batched offer" }
                }
            }

            if (offersToProcess.size > SMALL_DELAY_THRESHOLD) {
                try {
                    val runtime = Runtime.getRuntime()
                    val memoryUsage = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.maxMemory()
                    if (memoryUsage > MEMORY_GC_THRESHOLD) {
                        log.w { "High memory pressure detected during batch processing" }
                        delay(SMALL_DELAY * 2)  // Use a smaller multiplier to avoid excessive delays
                    } else {
                        delay(SMALL_DELAY)
                    }
                } catch (e: Exception) {
                    log.e(e) { "Error checking memory usage, failed to delay offer processing" }
                }
            }
        }

        // Single UI update for all new offers
        if (newOffers.isNotEmpty()) {
            _offerbookListItems.update { it + newOffers }
            val currentSize = _offerbookListItems.value.size
            log.i { "Batch processed $processedCount offers, total: $currentSize" }

            // Log memory pressure if list is getting large
            if (currentSize > 100 && currentSize % 50 == 0) {
                val mapSize = offerMapMutex.withLock { bisqEasyOfferbookMessageByOfferId.size }
                log.w { "MEMORY: Large offer list - UI: $currentSize, Map: $mapSize" }
            }
        }
    }

    private fun startMemoryMonitoring() {
        memoryMonitoringJob = launchIO {
            while (true) {
                delay(MEMORY_LOG_INTERVAL)
                try {
                    val runtime = Runtime.getRuntime()
                    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                    val maxMemory = runtime.maxMemory() / 1024 / 1024
                    val offerMapSize = offerMapMutex.withLock { bisqEasyOfferbookMessageByOfferId.size }
                    val offersListSize = _offerbookListItems.value.size
                    val observersCount = numOffersObservers.size

                    log.w { "MEMORY: Used ${usedMemory}MB/${maxMemory}MB, OfferMap: $offerMapSize, OffersList: $offersListSize, Observers: $observersCount" }

                    // Only suggest GC in critical situations (90%+) to avoid masking memory leaks
                    if (usedMemory > maxMemory * MEMORY_GC_THRESHOLD) {
                        log.w { "MEMORY: Critical memory usage detected (${usedMemory}MB/${maxMemory}MB), suggesting GC" }
                        suggestGCtoOS()
                    }
                } catch (e: Exception) {
                    log.e(e) { "Error in memory monitoring" }
                }
            }
        }
    }

    /**
     * suggests Garbage Collection to OS making sure we don't call it too often
     * TODO when the need to reuse memory management code arises, move to common helper object
     */
    private fun suggestGCtoOS() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGcTime > MIN_GC_INTERVAL) {
            // Note: System.gc() is a suggestion only, used here for P2P sync GC pressure relief
            // This is documented as necessary for heavy network sync workloads - see memory optimization PR
            // for node release builds we use manifest largeHeap flag which in general should be sufficient
            System.gc()
            lastGcTime = currentTime
        }
    }

    /**
     * Handle system memory pressure callbacks
     * Uses raw integer values instead of deprecated ComponentCallbacks2 constants
     *
     * Memory trim levels (from Android documentation):
     * - TRIM_MEMORY_COMPLETE (80): App in background, system extremely low on memory
     * - TRIM_MEMORY_MODERATE (60): App in background, system moderately low on memory
     * - TRIM_MEMORY_BACKGROUND (40): App just moved to background
     * - TRIM_MEMORY_UI_HIDDEN (20): App's UI no longer visible
     * - TRIM_MEMORY_RUNNING_CRITICAL (15): App running, system extremely low on memory
     * - TRIM_MEMORY_RUNNING_LOW (10): App running, system low on memory
     * - TRIM_MEMORY_RUNNING_MODERATE (5): App running, system moderately low on memory
     */
    fun onTrimMemory(level: Int) {
        when {
            level >= 80 || level == 15 -> { // COMPLETE or RUNNING_CRITICAL
                log.w { "MEMORY: Critical system memory pressure (level $level), clearing caches" }
                launchIO {
                    // Clear non-essential caches during critical memory pressure
                    val clearedOffers = offerMapMutex.withLock {
                        val size = bisqEasyOfferbookMessageByOfferId.size
                        if (size > MAP_CLEAR_THRESHOLD) {
                            // Keep only recent offers during memory pressure
                            val recentOffers = bisqEasyOfferbookMessageByOfferId.entries
                                .sortedByDescending { it.value.date }
                                .take(25)
                                .associate { it.key to it.value }
                            bisqEasyOfferbookMessageByOfferId.clear()
                            bisqEasyOfferbookMessageByOfferId.putAll(recentOffers)
                            size - recentOffers.size
                        } else 0
                    }
                    if (clearedOffers > 0) {
                        log.w { "MEMORY: Cleared $clearedOffers old offers due to memory pressure" }
                    }
                }
            }
            level >= 10 -> { // RUNNING_LOW or higher
                log.i { "MEMORY: System memory running low (level $level), reducing batch sizes" }
                // Could reduce batch processing sizes here if needed
            }
            else -> {
                log.d { "MEMORY: Minor memory trim request (level $level)" }
            }
        }
    }
}
