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
import kotlinx.coroutines.flow.MutableStateFlow
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
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import java.util.Date
import java.util.Optional


class NodeOffersServiceFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
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
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Life cycle
    override fun activate() {
        super.activate()

        observeSelectedChannel()
        observeMarketPrice()
        observeMarketListItems(_offerbookMarketItems)
    }

    override fun deactivate() {
        chatMessagesPin?.unbind()
        chatMessagesPin = null
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null
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
        // TODO Add more validation
        // In Bisq main we have that code in the bisqEasyOfferbookMessageService.isValid(message)
        // method
        /*
         public boolean isValid(BisqEasyOfferbookMessage message) {
        return isNotBanned(message) &&
                isNotIgnored(message) &&
                (isTextMessage(message) ||
                        isBuyOffer(message) ||
                        hasSellerSufficientReputation(message));
    }
         */
        return true
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
            MarketListItem.from(
                marketVO,
                channel.chatMessages.size,
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
}
