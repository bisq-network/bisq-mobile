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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import network.bisq.mobile.domain.service.offers.OfferFormattingUtil
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

    // Misc
    private var ignoredIdsJob: Job? = null

    private var selectedChannel: BisqEasyOfferbookChannel? = null
    private var marketPriceUpdateJob: Job? = null
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
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob = null
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
                bisqEasyOfferbookChannelService.deleteChatMessage(
                    offerbookMessage,
                    userIdentity.networkIdWithKeyPair
                ).join()
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

        val channel: BisqEasyOfferbookChannel =
            bisqEasyOfferbookChannelService.findChannel(market).get()
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
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
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
                (isTextMessage(message) || isBuyOffer(message) || hasSellerSufficientReputation(
                    message
                ))
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
                offerVO
            )

        // If we cannot determine required score (missing market prices), we err on the safe side
        // and do not filter by reputation to avoid hiding legitimate offers due to transient price lookups.
        if (requiredScore == null) return true

        val authorScore =
            reputationService.getReputationScore(message.authorUserProfileId).totalScore
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
            val formattedPrice =
                marketPriceServiceFacade.selectedMarketPriceItem.value!!.formattedPrice
            _selectedOfferbookMarket.value.setFormattedPrice(formattedPrice)
        }
    }

    private fun scheduleOffersPriceRefresh() {
        marketPriceUpdateJob?.cancel()
        marketPriceUpdateJob = serviceScope.launch(Dispatchers.Default) {
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
