package network.bisq.mobile.android.node.service.offers

import bisq.account.payment_method.BitcoinPaymentMethod
import bisq.account.payment_method.BitcoinPaymentMethodUtil
import bisq.account.payment_method.FiatPaymentMethod
import bisq.account.payment_method.FiatPaymentMethodUtil
import bisq.bisq_easy.BisqEasyServiceUtil
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.ChatMessageType
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
import bisq.user.banned.BannedUserService
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade

import kotlinx.coroutines.flow.update
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.mapping.OfferItemPresentationVOFactory
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import java.util.Date
import java.util.Optional


class NodeOffersServiceFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : OffersServiceFacade() {
    // Dependencies
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val reputationService: ReputationService by lazy { applicationService.userService.get().reputationService }
    private val bannedUserService: BannedUserService by lazy { applicationService.userService.get().bannedUserService }
    private val bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelSelectionService }

//  TODO restore for usage of v2.1.8
//    private val bisqEasyOfferbookMessageService: BisqEasyOfferbookMessageService by lazy { applicationService.bisqEasyService.get().bisqEasyOfferbookMessageService }


    // Misc
    private var ignoredIdsJob: Job? = null

    private var selectedChannel: BisqEasyOfferbookChannel? = null
    private val bisqEasyOfferbookMessageByOfferId: MutableMap<String, BisqEasyOfferbookMessage> = mutableMapOf()
    private var marketPriceUpdateJob: Job? = null
    private val offerMapMutex = Mutex()
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Life cycle
    override fun activate() {
        super.activate()

        // We set channel to null to avoid that our _offerbookMarketItems gets filled initially
        // React to ignore/unignore to update both lists and counts immediately
        observeIgnoredProfiles()

        // to avoid memory and cpu pressure at startup.

        // Memory and scheduling helpers
        var memoryMonitoringJob: Job? = null
        var lastGcTime: Long = 0L
        val SMALL_DELAY_THRESHOLD = 50
        val SMALL_DELAY = 20L
        val MEMORY_LOG_INTERVAL = 10_000L
        val MEMORY_GC_THRESHOLD = 0.90
        val MAP_CLEAR_THRESHOLD = 1000
        val MIN_GC_INTERVAL = 30_000L

        // We only want to fill it when we select a market.
        bisqEasyOfferbookChannelSelectionService.selectChannel(null)

        observeSelectedChannel()
        observeMarketPrice()
        observeMarketListItems(_offerbookMarketItems)
    }

    private fun observeIgnoredProfiles() {
        ignoredIdsJob?.cancel()
        ignoredIdsJob = serviceScope.launch {
            userProfileServiceFacade.ignoredProfileIds.collectLatest {
                // Re-filter current selected channel's list items
                selectedChannel?.let { ch ->
                    val listItems = ch.chatMessages
                        .filter { it.hasBisqEasyOffer() }
                        .filter { isValidOfferbookMessage(it) }
                        .map { createOfferItemPresentationModel(it) }
                        .distinctBy { it.bisqEasyOffer.id }
                    _offerbookListItems.value = listItems
                }
                // Refresh counts for all markets
                numOffersObservers.forEach { it.refresh() }
            }
        }
    }

    override fun deactivate() {
        chatMessagesPin?.unbind()
        chatMessagesPin = null
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null
        ignoredIdsJob?.cancel()
        ignoredIdsJob = null
        numOffersObservers.forEach { it.dispose() }
        numOffersObservers.clear()

        super.deactivate()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        val market = Mappings.MarketMapping.toBisq2Model(marketListItem.market)
        val channelOptional = bisqEasyOfferbookChannelService.findChannel(market)

        if (!channelOptional.isPresent) {
            log.e { "No channel found for market ${market.marketCodes}" }
            return
        }

        val channel = channelOptional.get()
        bisqEasyOfferbookChannelSelectionService.selectChannel(channel)
        marketPriceServiceFacade.selectMarket(marketListItem)
    }

    override suspend fun deleteOffer(offerId: String): Result<Boolean> {
        try {
            val optionalOfferbookMessage: Optional<BisqEasyOfferbookMessage> = bisqEasyOfferbookChannelService.findMessageByOfferId(offerId)
            check(optionalOfferbookMessage.isPresent) { "Could not find offer for offer ID $offerId" }
            val offerbookMessage: BisqEasyOfferbookMessage = optionalOfferbookMessage.get()
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
            Result.success(offerId)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer: ${e.message}" }
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

    /////////////////////////////////////////////////////////////////////////////
    // Market Channel
    /////////////////////////////////////////////////////////////////////////////

    private fun observeSelectedChannel() {
        selectedChannelPin?.unbind()
        selectedChannelPin = bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
            if (channel == null) {
                selectedChannel = channel
                chatMessagesPin?.unbind()
            } else if (channel is BisqEasyOfferbookChannel) {
                selectedChannel = channel
                marketPriceService.setSelectedMarket(channel.market)
                val marketVO = Mappings.MarketMapping.fromBisq2Model(channel.market)
                _selectedOfferbookMarket.value = OfferbookMarket(marketVO)
                updateMarketPrice()

                observeChatMessages(channel)
            } else {
                log.w { "Selected channel is not a BisqEasyOfferbookChannel: ${channel::class.simpleName}" }
            }
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    // OfferbookListItems
    /////////////////////////////////////////////////////////////////////////////

    private fun observeChatMessages(channel: BisqEasyOfferbookChannel) {
        _offerbookListItems.update { emptyList() }

        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = channel.chatMessages
        chatMessagesPin?.unbind()
        chatMessagesPin =
            chatMessages.addObserver(object : CollectionObserver<BisqEasyOfferbookMessage> {
                // We get all already existing offers applied at channel selection
                override fun addAll(values: Collection<BisqEasyOfferbookMessage>) {
                    val listItems: List<OfferItemPresentationModel> = values
                        .filter { it.hasBisqEasyOffer() }
                        .filter { isValidOfferbookMessage(it) }
                        .map { createOfferItemPresentationModel(it) }
                    _offerbookListItems.update { current -> (current + listItems).distinctBy { it.bisqEasyOffer.id } }
                }

                // Newly added messages
                override fun add(message: BisqEasyOfferbookMessage) {
                    if (!message.hasBisqEasyOffer() || !isValidOfferbookMessage(message)) {
                        return
                    }
                    val listItem = createOfferItemPresentationModel(message)
                    _offerbookListItems.update { current -> (current + listItem).distinctBy { it.bisqEasyOffer.id } }
                }

                override fun remove(message: Any) {
                    if (message is BisqEasyOfferbookMessage && message.bisqEasyOffer.isPresent) {
                        val offerId = message.bisqEasyOffer.get().id
                        _offerbookListItems.update { current ->
                            val item = current.firstOrNull { it.bisqEasyOffer.id == offerId }
                            if (item != null) {
                                log.i { "Removed offer: $offerId, remaining offers: ${current.size - 1}" }
                                current - item
                            } else current
                        }
                    }
                }

                override fun clear() {
                    _offerbookListItems.update { emptyList() }
                }
            })
    }

    private fun createOfferItemPresentationModel(bisqEasyOfferbookMessage: BisqEasyOfferbookMessage): OfferItemPresentationModel {
        val offerItemPresentationDto = OfferItemPresentationVOFactory.create(
            userProfileService,
            userIdentityService,
            marketPriceService,
            reputationService,
            bisqEasyOfferbookMessage
        )
        return OfferItemPresentationModel(offerItemPresentationDto)
    }

    private fun isValidOfferbookMessage(message: BisqEasyOfferbookMessage): Boolean {
        // Mirrors Bisq main: see bisqEasyOfferbookMessageService.isValid(message)
        return isNotBanned(message) &&
            isNotIgnored(message) &&
            (isTextMessage(message) || isBuyOffer(message) || hasSellerSufficientReputation(message))
    }

    private fun isNotBanned(message: BisqEasyOfferbookMessage): Boolean {
        val authorUserProfileId = message.authorUserProfileId
        return !bannedUserService.isUserProfileBanned(authorUserProfileId)
    }

    private fun isNotIgnored(message: BisqEasyOfferbookMessage): Boolean {
        val authorUserProfileId = message.authorUserProfileId
        return !userProfileService.isChatUserIgnored(authorUserProfileId)
    }

    private fun isTextMessage(message: BisqEasyOfferbookMessage): Boolean {
        if (message.chatMessageType == ChatMessageType.TEXT) return true
        return message.text.isPresent && !message.bisqEasyOffer.isPresent
    }

    private fun isBuyOffer(message: BisqEasyOfferbookMessage): Boolean {
        val offerOpt = message.bisqEasyOffer
        return offerOpt.isPresent && offerOpt.get().direction == Direction.BUY
    }

    private fun hasSellerSufficientReputation(message: BisqEasyOfferbookMessage): Boolean {
        // Only meaningful when there's an offer attached
        val offerOpt = message.bisqEasyOffer
        if (!offerOpt.isPresent) return false

        val offer = offerOpt.get()

        // BUY offers are always allowed upstream; SELL offers require additional reputation checks.
        // We keep semantic parity with the main app by requiring the author's reputation to meet
        // the reputation threshold implied by the offer's min/fixed amount.
        val directionEnum = Mappings.DirectionMapping.fromBisq2Model(offer.direction)
        if (directionEnum == DirectionEnum.BUY) return true

        // Compute required seller reputation based on offer amount in fiat using our domain util.
        val offerVO = Mappings.BisqEasyOfferMapping.fromBisq2Model(offer)
        val requiredScore = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
            marketPriceServiceFacade,
            offerVO
        )

        // If we cannot determine required score (missing market prices), we err on the safe side
        // and do not filter by reputation to avoid hiding legitimate offers due to transient price lookups.
        if (requiredScore == null) return true

        val authorScore = reputationService.getReputationScore(message.authorUserProfileId).totalScore
        return authorScore >= requiredScore
    }


    /////////////////////////////////////////////////////////////////////////////
    // Markets
    /////////////////////////////////////////////////////////////////////////////

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
            val count = channel.chatMessages.count { isNotEmptyAndValid(it) }
            MarketListItem.from(
                marketVO,
                count,
            )
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
                    messageFilter = { msg -> isNotEmptyAndValid(msg) },
                    setNumOffers = { numOffers ->
                        val safeNumOffers = numOffers
                        // Rebuild the list immutably
                        itemsFlow.value = itemsFlow.value.map {
                            if (it.market == marketVO) it.copy(numOffers = safeNumOffers) else it
                        }
                    },
                )
                numOffersObservers.add(numOffersObserver)
                val initialCount = channel.chatMessages.count { isNotEmptyAndValid(it) }
                log.d { "Added market ${market.marketCodes} with initial offers count: $initialCount" }
            } else {
                log.d { "Skipped market ${market.marketCodes} - not in marketPriceByCurrencyMap" }
            }
        }
        log.d { "Filled market list items, count: ${itemsFlow.value.size}" }
    }

    private fun isNotEmptyAndValid(message: BisqEasyOfferbookMessage): Boolean =
        message.hasBisqEasyOffer() && isValidOfferbookMessage(message)

    private fun observeMarketPrice() {
        marketPricePin = marketPriceService.marketPriceByCurrencyMap.addObserver(Runnable {
            marketPriceService.findMarketPriceQuote(marketPriceService.selectedMarket.get())
            updateMarketPrice()
            // Debounced per-offer updates when market price changes
            scheduleOffersPriceRefresh()
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

    private fun scheduleOffersPriceRefresh() {
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob = serviceScope.launch(Dispatchers.Default) {
            try {
                delay(150)
                refreshOffersFormattedValues()
            } catch (e: Exception) {
                log.e(e) { "Error scheduling offers price refresh" }
            }
        }
    }

    private fun refreshOffersFormattedValues() {
        val marketItem = marketPriceServiceFacade.selectedMarketPriceItem.value ?: return
        val currentOffers = _offerbookListItems.value
        if (currentOffers.isEmpty()) return
        refreshOffersFormattedValuesForTest(currentOffers, marketItem)
    }

    // Visible for JVM tests to avoid loading AndroidApplicationService.Provider
    internal fun refreshOffersFormattedValuesForTest(
        offers: List<OfferItemPresentationModel>,
        marketItem: MarketPriceItem
    ) {
        offers.forEach { model ->
            val offerVO = model.bisqEasyOffer
            val priceSpecVO = offerVO.priceSpec

            // Only offers depending on market price need updates
            if (priceSpecVO is FixPriceSpecVO) return@forEach

            try {
                // 1) Price string (shared KMP formatter for consistency across Node/Client)
                val priceQuoteVO = priceSpecVO.getPriceQuoteVO(marketItem)
                val newFormattedPrice = PriceQuoteFormatter.format(priceQuoteVO, useLowPrecision = true, withCode = true)
                model.updateFormattedPrice(newFormattedPrice)
            } catch (e: Exception) {
                log.e(e) { "Error updating formatted price for offer ${offerVO.id}" }
            }

            try {
                // 2) Base amount string (compute from amountSpec + price quote)
                val priceQuoteVO = priceSpecVO.getPriceQuoteVO(marketItem)
                val newFormattedBaseAmount = when (val amountSpec = offerVO.amountSpec) {
                    is QuoteSideFixedAmountSpecVO -> {
                        val quoteMonetary = FiatVOFactory.run { from(amountSpec.amount, offerVO.market.quoteCurrencyCode) }
                        val baseMonetary = priceQuoteVO.toBaseSideMonetary(quoteMonetary)
                        AmountFormatter.formatAmount(baseMonetary, useLowPrecision = false, withCode = true)
                    }
                    is QuoteSideRangeAmountSpecVO -> {
                        val minQuote = FiatVOFactory.run { from(amountSpec.minAmount, offerVO.market.quoteCurrencyCode) }
                        val maxQuote = FiatVOFactory.run { from(amountSpec.maxAmount, offerVO.market.quoteCurrencyCode) }
                        val minBase = priceQuoteVO.toBaseSideMonetary(minQuote)
                        val maxBase = priceQuoteVO.toBaseSideMonetary(maxQuote)
                        AmountFormatter.formatRangeAmount(minBase, maxBase, useLowPrecision = false, withCode = true)
                    }
                    else -> {
                        // Base-side specs do not depend on market price for base amount; keep previous value
                        model.formattedBaseAmount.value
                    }
                }
                model.updateFormattedBaseAmount(newFormattedBaseAmount)
            } catch (e: Exception) {
                // Per requirement: keep previous formatted values if unavailable or on error
                log.e(e) { "Error updating formatted base amount for offer ${offerVO.id}" }
            }
        }
    }

}



// Top-level helper visible to JVM tests to avoid AndroidApplicationService.Provider
internal fun refreshOffersFormattedValuesForTest(
    offers: List<OfferItemPresentationModel>,
    marketItem: MarketPriceItem
) {
    offers.forEach { model ->
        val offerVO = model.bisqEasyOffer
        val priceSpecVO = offerVO.priceSpec
        if (priceSpecVO is FixPriceSpecVO) return@forEach
        try {
            val priceQuoteVO = priceSpecVO.getPriceQuoteVO(marketItem)
            val newFormattedPrice = PriceQuoteFormatter.format(priceQuoteVO, useLowPrecision = true, withCode = true)
            model.updateFormattedPrice(newFormattedPrice)
        } catch (_: Exception) {
            // ignore errors in test helper
        }
        try {
            val priceQuoteVO = priceSpecVO.getPriceQuoteVO(marketItem)
            val newFormattedBaseAmount = when (val amountSpec = offerVO.amountSpec) {
                is QuoteSideFixedAmountSpecVO -> {
                    val quoteMonetary = FiatVOFactory.run { from(amountSpec.amount, offerVO.market.quoteCurrencyCode) }
                    val baseMonetary = priceQuoteVO.toBaseSideMonetary(quoteMonetary)
                    AmountFormatter.formatAmount(baseMonetary, useLowPrecision = false, withCode = true)
                }
                is QuoteSideRangeAmountSpecVO -> {
                    val minQuote = FiatVOFactory.run { from(amountSpec.minAmount, offerVO.market.quoteCurrencyCode) }
                    val maxQuote = FiatVOFactory.run { from(amountSpec.maxAmount, offerVO.market.quoteCurrencyCode) }
                    val minBase = priceQuoteVO.toBaseSideMonetary(minQuote)
                    val maxBase = priceQuoteVO.toBaseSideMonetary(maxQuote)
                    AmountFormatter.formatRangeAmount(minBase, maxBase, useLowPrecision = false, withCode = true)
                }
                else -> model.formattedBaseAmount.value
            }
            model.updateFormattedBaseAmount(newFormattedBaseAmount)
        } catch (_: Exception) {
            // ignore errors in test helper
        }
    }

}
