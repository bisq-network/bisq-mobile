package network.bisq.mobile.node.common.domain.service.offers

import bisq.account.payment_method.BitcoinPaymentMethod
import bisq.account.payment_method.BitcoinPaymentMethodUtil
import bisq.account.payment_method.fiat.FiatPaymentMethod
import bisq.account.payment_method.fiat.FiatPaymentMethodUtil
import bisq.bisq_easy.BisqEasyServiceUtil
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.ChatMessageType
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.market.Market
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.network.p2p.services.data.BroadcastResult
import bisq.offer.Direction
import bisq.offer.amount.spec.AmountSpec
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.price.spec.PriceSpec
import bisq.user.banned.BannedUserService
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OfferFormattingUtil
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.mapping.OfferItemPresentationVOFactory
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import java.util.Date
import java.util.Optional

class NodeOffersServiceFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
) : OffersServiceFacade() {
    companion object {
        private val DEFAULT_MARKET = Market("BTC", "USD", "Bitcoin", "US Dollar")
    }

    // Dependencies
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val reputationService: ReputationService by lazy { applicationService.userService.get().reputationService }
    private val bannedUserService: BannedUserService by lazy { applicationService.userService.get().bannedUserService }
    private val bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelSelectionService }

    // Misc
    private var ignoredIdsJob: Job? = null

    private var selectedChannel: BisqEasyOfferbookChannel? = null
    private var marketPriceUpdateJob: Job? = null
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Job for processing offers asynchronously - cancelled when switching markets
    private var offerProcessingJob: Job? = null

    // Life cycle
    override suspend fun activate() {
        super.activate()

        // React to ignore/unignore to update both lists and counts immediately
        observeIgnoredProfiles()

        // Restore the previously selected market from settings (if any) and select its channel
        // This avoids loading ALL offers at startup (memory/CPU optimization)
        // while still showing offers for the user's last selected market
        restoreAndSelectChannel()

        observeSelectedChannel()
        observeMarketPrice()
        observeMarketListItems(_offerbookMarketItems)
    }

    private suspend fun restoreAndSelectChannel() {
        var marketSelectionRestored = false
        runCatching {
            val settings = settingsRepository.fetch()
            val marketCode = settings.selectedMarketCode

            if (marketCode.isNullOrEmpty()) {
                log.d { "No selected market code to restore" }
            } else {
                val parts = marketCode.split("/")

                if (parts.size == 2) {
                    val baseCurrency = parts[0]
                    val quoteCurrency = parts[1]
                    val marketVO = MarketVO(baseCurrency, quoteCurrency)
                    val market = Mappings.MarketMapping.toBisq2Model(marketVO)
                    val channelOptional = bisqEasyOfferbookChannelService.findChannel(market)

                    if (channelOptional.isPresent) {
                        log.d { "Restoring selected channel for market: ${market.marketCodes}" }
                        bisqEasyOfferbookChannelSelectionService.selectChannel(channelOptional.get())
                        marketSelectionRestored = true
                    } else {
                        log.w { "Could not find channel for restored market: ${market.marketCodes}" }
                    }
                }
            }
        }.onFailure { e ->
            log.w(e) { "Failed to restore selected market from settings" }
        }

        // (fresh install or error), select the default market (BTC/USD)
        if (!marketSelectionRestored) {
            restoreAndSelectDefaultChannel()
        }
    }

    private fun restoreAndSelectDefaultChannel() {
        val channelOptional = bisqEasyOfferbookChannelService.findChannel(DEFAULT_MARKET)
        if (channelOptional.isPresent) {
            log.d { "No saved market found, selecting default market: ${DEFAULT_MARKET.marketCodes}" }
            bisqEasyOfferbookChannelSelectionService.selectChannel(channelOptional.get())
        } else {
            log.w { "Could not find default BTC/USD channel, setting channel to null" }
            bisqEasyOfferbookChannelSelectionService.selectChannel(null)
        }
    }

    private fun observeIgnoredProfiles() {
        ignoredIdsJob?.cancel()
        ignoredIdsJob =
            serviceScope.launch {
                userProfileServiceFacade.ignoredProfileIds.collectLatest {
                    // Re-filter current selected channel's list items
                    selectedChannel?.let { ch ->
                        val listItems =
                            ch.chatMessages
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

    override suspend fun deactivate() {
        chatMessagesPin?.unbind()
        chatMessagesPin = null
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob = null
        ignoredIdsJob?.cancel()
        ignoredIdsJob = null
        offerProcessingJob?.cancel()
        offerProcessingJob = null
        numOffersObservers.forEach { it.dispose() }
        numOffersObservers.clear()

        super.deactivate()
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem): Result<Unit> =
        runCatching {
            val market = Mappings.MarketMapping.toBisq2Model(marketListItem.market)
            val channelOptional = bisqEasyOfferbookChannelService.findChannel(market)

            if (!channelOptional.isPresent) {
                throw IllegalStateException("No channel found for market ${market.marketCodes}")
            }

            val channel = channelOptional.get()
            bisqEasyOfferbookChannelSelectionService.selectChannel(channel)
            marketPriceServiceFacade.selectMarket(marketListItem).getOrThrow()
        }.onFailure { e ->
            log.e("Failed to select offerbook market: ${marketListItem.market}", e)
        }

    override suspend fun deleteOffer(offerId: String): Result<Boolean> {
        try {
            // significant CPU work and possibly blocking action
            return withContext(Dispatchers.IO) {
                val optionalOfferbookMessage: Optional<BisqEasyOfferbookMessage> =
                    bisqEasyOfferbookChannelService.findMessageByOfferId(offerId)
                check(optionalOfferbookMessage.isPresent) { "Could not find offer for offer ID $offerId" }
                val offerbookMessage: BisqEasyOfferbookMessage = optionalOfferbookMessage.get()
                val authorUserProfileId: String = offerbookMessage.authorUserProfileId
                val optionalUserIdentity = userIdentityService.findUserIdentity(authorUserProfileId)
                check(optionalUserIdentity.isPresent) { "UserIdentity for authorUserProfileId $authorUserProfileId not found" }
                val userIdentity = optionalUserIdentity.get()
                check(userIdentity == userIdentityService.selectedUserIdentity) { "Selected selectedUserIdentity does not match the offers authorUserIdentity" }
                val broadcastResult: BroadcastResult =
                    bisqEasyOfferbookChannelService
                        .deleteChatMessage(
                            offerbookMessage,
                            userIdentity.networkIdWithKeyPair,
                        ).await()
                val broadcastResultNotEmpty = broadcastResult.isNotEmpty()
                if (!broadcastResultNotEmpty) {
                    log.w { "Delete offer message was not broadcast to network. Maybe there are no peers connected." }
                }
                Result.success(broadcastResultNotEmpty)
            }
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
        supportedLanguageCodes: Set<String>,
    ): Result<String> =
        try {
            // significant CPU work and possibly blocking action, otherwise Default dispatcher
            // would be a better choice
            val offerId =
                withContext(Dispatchers.IO) {
                    createOffer(
                        Mappings.DirectionMapping.toBisq2Model(direction),
                        Mappings.MarketMapping.toBisq2Model(market),
                        bitcoinPaymentMethods.map { BitcoinPaymentMethodUtil.getPaymentMethod(it) },
                        fiatPaymentMethods.map { FiatPaymentMethodUtil.getPaymentMethod(it) },
                        Mappings.AmountSpecMapping.toBisq2Model(amountSpec),
                        Mappings.PriceSpecMapping.toBisq2Model(priceSpec),
                        ArrayList<String>(supportedLanguageCodes),
                    )
                }
            Result.success(offerId)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer: ${e.message}" }
            Result.failure(e)
        }

    // Private
    private suspend fun createOffer(
        direction: Direction,
        market: Market,
        bitcoinPaymentMethods: List<BitcoinPaymentMethod>,
        fiatPaymentMethods: List<FiatPaymentMethod>,
        amountSpec: AmountSpec,
        priceSpec: PriceSpec,
        supportedLanguageCodes: List<String>,
    ): String {
        val userIdentity: UserIdentity = userIdentityService.selectedUserIdentity
        val chatMessageText =
            BisqEasyServiceUtil.createOfferBookMessageFromPeerPerspective(
                userIdentity.nickName,
                marketPriceService,
                direction,
                market,
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                amountSpec,
                priceSpec,
            )
        val userProfile = userIdentity.userProfile
        val bisqEasyOffer =
            BisqEasyOffer(
                userProfile.networkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                userProfile.terms,
                supportedLanguageCodes,
                BuildNodeConfig.TRADE_PROTOCOL_VERSION,
            )

        val channel: BisqEasyOfferbookChannel =
            bisqEasyOfferbookChannelService.findChannel(market).get()
        val myOfferMessage =
            BisqEasyOfferbookMessage(
                channel.id,
                userProfile.id,
                Optional.of(bisqEasyOffer),
                Optional.of(chatMessageText),
                Optional.empty(),
                Date().time,
                false,
            )

        bisqEasyOfferbookChannelService.publishChatMessage(myOfferMessage, userIdentity).await()
        return bisqEasyOffer.id
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Market Channel
    // ///////////////////////////////////////////////////////////////////////////

    private fun observeSelectedChannel() {
        selectedChannelPin?.unbind()
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
                if (channel == null) {
                    selectedChannel = channel
                    chatMessagesPin?.unbind()
                    _isOfferbookLoading.value = false
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

    // ///////////////////////////////////////////////////////////////////////////
    // OfferbookListItems
    // ///////////////////////////////////////////////////////////////////////////

    private fun observeChatMessages(channel: BisqEasyOfferbookChannel) {
        // Cancel any ongoing offer processing from previous market selection
        offerProcessingJob?.cancel()

        _isOfferbookLoading.value = true
        _offerbookListItems.update { emptyList() }

        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = channel.chatMessages
        chatMessagesPin?.unbind()
        chatMessagesPin =
            chatMessages.addObserver(
                object : CollectionObserver<BisqEasyOfferbookMessage> {
                    // We get all already existing offers applied at channel selection
                    override fun addAll(values: Collection<BisqEasyOfferbookMessage>) {
                        val currentChannel = channel
                        // Process offers asynchronously to avoid blocking the main thread
                        // This prevents ANRs when selecting markets with many offers
                        // Using Default dispatcher for CPU-intensive work (formatting, reputation calculations)
                        offerProcessingJob =
                            serviceScope.launch(Dispatchers.Default) {
                                try {
                                    val listItems: List<OfferItemPresentationModel> =
                                        values
                                            .filter { it.hasBisqEasyOffer() }
                                            .filter { isValidOfferbookMessage(it) }
                                            .map { createOfferItemPresentationModel(it) }

                                    // Update UI state on main thread
                                    withContext(Dispatchers.Main) {
                                        // Only update if we're still on the same channel
                                        if (selectedChannel == currentChannel) {
                                            _offerbookListItems.update { current ->
                                                (current + listItems).distinctBy { it.bisqEasyOffer.id }
                                            }
                                        }
                                    }
                                } finally {
                                    // Always reset loading state, even if job is cancelled
                                    // NonCancellable ensures cleanup runs even during cancellation
                                    withContext(NonCancellable + Dispatchers.Main) {
                                        _isOfferbookLoading.value = false
                                    }
                                }
                            }
                    }

                    // Newly added messages
                    override fun add(message: BisqEasyOfferbookMessage) {
                        if (!message.hasBisqEasyOffer() || !isValidOfferbookMessage(message)) {
                            return
                        }
                        val currentChannel = channel
                        // Process single offer asynchronously to avoid blocking main thread
                        // Using Default dispatcher for CPU-intensive work (formatting, reputation calculations)
                        serviceScope.launch(Dispatchers.Default) {
                            val listItem = createOfferItemPresentationModel(message)
                            withContext(Dispatchers.Main) {
                                // Only update if we're still on the same channel
                                if (selectedChannel == currentChannel) {
                                    _offerbookListItems.update { current ->
                                        (current + listItem).distinctBy { it.bisqEasyOffer.id }
                                    }
                                }
                            }
                        }
                    }

                    override fun remove(message: Any) {
                        if (message is BisqEasyOfferbookMessage && message.bisqEasyOffer.isPresent) {
                            val offerId = message.bisqEasyOffer.get().id
                            _offerbookListItems.update { current ->
                                val item = current.firstOrNull { it.bisqEasyOffer.id == offerId }
                                if (item != null) {
                                    log.i { "Removed offer: $offerId, remaining offers: ${current.size - 1}" }
                                    current - item
                                } else {
                                    current
                                }
                            }
                        }
                    }

                    override fun clear() {
                        _offerbookListItems.update { emptyList() }
                    }
                },
            )
    }

    private fun createOfferItemPresentationModel(bisqEasyOfferbookMessage: BisqEasyOfferbookMessage): OfferItemPresentationModel {
        val offerItemPresentationDto =
            OfferItemPresentationVOFactory.create(
                userProfileService,
                userIdentityService,
                marketPriceService,
                reputationService,
                bisqEasyOfferbookMessage,
            )
        return OfferItemPresentationModel(offerItemPresentationDto)
    }

    private fun isValidOfferbookMessage(message: BisqEasyOfferbookMessage): Boolean {
        // Mirrors Bisq main: see bisqEasyOfferbookMessageService.isValid(message)
        return isNotBanned(message) &&
            isNotIgnored(message) &&
            (
                isTextMessage(message) || isBuyOffer(message) ||
                    hasSellerSufficientReputation(
                        message,
                    )
            )
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
        val requiredScore =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
                marketPriceServiceFacade,
                offerVO,
            )

        // If we cannot determine required score (missing market prices), we err on the safe side
        // and do not filter by reputation to avoid hiding legitimate offers due to transient price lookups.
        if (requiredScore == null) return true

        val authorScore =
            reputationService.getReputationScore(message.authorUserProfileId).totalScore
        return authorScore >= requiredScore
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Markets
    // ///////////////////////////////////////////////////////////////////////////

    private fun observeMarketListItems(itemsFlow: MutableStateFlow<List<MarketListItem>>) {
        log.d { "Observing market list items" }
        numOffersObservers.forEach { it.dispose() }
        numOffersObservers.clear()

        val channels = bisqEasyOfferbookChannelService.channels
        val initialItems =
            channels.map { channel ->
                val marketVO =
                    MarketVO(
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
            val marketVO =
                MarketVO(
                    channel.market.baseCurrencyCode,
                    channel.market.quoteCurrencyCode,
                    channel.market.baseCurrencyName,
                    channel.market.quoteCurrencyName,
                )
            val market = Mappings.MarketMapping.toBisq2Model(marketVO)
            if (marketPriceService.marketPriceByCurrencyMap.isEmpty() ||
                marketPriceService.marketPriceByCurrencyMap.containsKey(market)
            ) {
                val numOffersObserver =
                    NumOffersObserver(
                        channel,
                        messageFilter = { msg -> isNotEmptyAndValid(msg) },
                        setNumOffers = { numOffers ->
                            val safeNumOffers = numOffers
                            // Rebuild the list immutably
                            itemsFlow.value =
                                itemsFlow.value.map {
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

    private fun isNotEmptyAndValid(message: BisqEasyOfferbookMessage): Boolean = message.hasBisqEasyOffer() && isValidOfferbookMessage(message)

    private fun observeMarketPrice() {
        marketPricePin =
            marketPriceService.marketPriceByCurrencyMap.addObserver(
                Runnable {
                    marketPriceService.findMarketPriceQuote(marketPriceService.selectedMarket.get())
                    updateMarketPrice()
                    // Debounced per-offer updates when market price changes
                    scheduleOffersPriceRefresh()
                },
            )
    }

    private fun updateMarketPrice() {
        if (marketPriceServiceFacade.selectedMarketPriceItem.value != null) {
            val formattedPrice =
                marketPriceServiceFacade.selectedMarketPriceItem.value!!.formattedPrice
            _selectedOfferbookMarket.value.setFormattedPrice(formattedPrice)
        }
    }

    private fun scheduleOffersPriceRefresh() {
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob =
            serviceScope.launch(Dispatchers.Default) {
                try {
                    // Debounce to avoid UI churn during high-frequency price ticks
                    delay(MARKET_TICK_DEBOUNCE_MS)
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
        OfferFormattingUtil.updateOffersFormattedValues(currentOffers, marketItem)
    }
}
