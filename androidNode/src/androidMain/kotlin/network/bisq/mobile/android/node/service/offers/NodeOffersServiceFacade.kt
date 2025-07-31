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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import java.util.Date
import java.util.Optional


class NodeOffersServiceFacade(
    applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userRepository: UserRepository
) : OffersServiceFacade() {

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

    // Life cycle
    override fun activate() {
        log.d { "Activating NodeOffersServiceFacade" }
        super<OffersServiceFacade>.activate()

        observeSelectedChannel()
        observeMarketPrice()
        if (numOffersObservers.isNotEmpty())  {
            numOffersObservers.forEach { it.resume() }
        } else {
            observeMarketListItems(_offerbookMarketItems)
        }
        log.d { "NodeOffersServiceFacade activated, numOffersObservers: ${numOffersObservers.size}" }
    }

    override fun deactivate() {
        log.d { "Deactivating NodeOffersServiceFacade" }
        chatMessagesPin?.unbind()
        chatMessagesPin = null
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null
        numOffersObservers.forEach { it.dispose() }
        log.d { "NodeOffersServiceFacade deactivated" }

        super<OffersServiceFacade>.deactivate()
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
        return Result.success(offerId)
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
                serviceScope.launch(Dispatchers.Default) {
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
                        log.d { "Ignoring message without offer in ${channel.market.marketCodes}" }
                        return
                    }
                    serviceScope.launch(Dispatchers.Default) {
                        val offerId = message.bisqEasyOffer.get().id
                        log.d { "Processing offer message: $offerId in ${channel.market.marketCodes}" }
                        if (!offerMessagesContainsKey(offerId) && isValidOfferMessage(message)) {
                            val offerItemPresentationDto: OfferItemPresentationDto = createOfferListItem(message)
                            val offerItemPresentationModel = OfferItemPresentationModel(offerItemPresentationDto)
                            _offerbookListItems.update { it + offerItemPresentationModel }
                            putOfferMessage(offerId, message)
                            log.i { "Added offer $offerId to list for ${channel.market.marketCodes}, total offers: ${_offerbookListItems.value.size}" }
                        } else {
                            log.d { "Skipped offer $offerId in ${channel.market.marketCodes} - already exists: ${offerMessagesContainsKey(offerId)}, valid: ${isValidOfferMessage(message)}" }
                        }
                    }
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
                            serviceScope.launch(Dispatchers.Default) { removeOfferMessage(offerId) }
                            log.i { "Removed offer: $offerId, remaining offers: ${_offerbookListItems.value.size}" }
                        }
                    }
                }

                override fun clear() {
                    log.d { "Clearing all offer messages" }
                    _offerbookListItems.value = emptyList()
                    serviceScope.launch(Dispatchers.Default) { clearOfferMessages() }
                }
            })
        
        log.d { "Chat messages observer added for ${channel.market.marketCodes}, pin: $chatMessagesPin" }
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
            log.d { "Clearing offer messages map, current size: ${bisqEasyOfferbookMessageByOfferId.size}" }
            bisqEasyOfferbookMessageByOfferId.clear()
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
}
