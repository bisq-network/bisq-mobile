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
import kotlinx.coroutines.flow.StateFlow
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
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.utils.Logging
import java.util.Date
import java.util.Optional


class NodeOffersServiceFacade(
    applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) :
    OffersServiceFacade, Logging {

    // Dependencies
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val reputationService: ReputationService by lazy { applicationService.userService.get().reputationService }
    private val bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService by lazy { applicationService.chatService.get().bisqEasyOfferbookChannelSelectionService }

    // Properties
    private val _offerbookListItems = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    override val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> get() = _offerbookListItems

    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    private val _offerbookMarketItems: List<MarketListItem> by lazy { fillMarketListItems() }
    override val offerbookMarketItems: List<MarketListItem> get() = _offerbookMarketItems

    // Misc
    private var selectedChannel: BisqEasyOfferbookChannel? = null
    private val bisqEasyOfferbookMessages: MutableSet<BisqEasyOfferbookMessage> = mutableSetOf()
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Life cycle
    override fun activate() {
        observeSelectedChannel()
        observeMarketPrice()
        numOffersObservers.forEach { it.resume() }
    }

    override fun deactivate() {
        chatMessagesPin?.unbind()
        chatMessagesPin = null
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null

        numOffersObservers.forEach { it.dispose() }
    }

    // API
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        val market = Mappings.MarketMapping.toBisq2Model(marketListItem.market)
        bisqEasyOfferbookChannelService.findChannel(market).ifPresent {
            bisqEasyOfferbookChannelSelectionService.selectChannel(it)
        }
        //todo marketPriceServiceFacade should not be managed here but on a higher level or from the presenter
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
            supportedLanguageCodes
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
        selectedChannelPin = bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
            if (channel == null) {
                selectedChannel = channel
            } else if (channel is BisqEasyOfferbookChannel) {
                selectedChannel = channel
                marketPriceService.setSelectedMarket(channel.market)
                val marketVO = Mappings.MarketMapping.fromBisq2Model(channel.market)
                _selectedOfferbookMarket.value = OfferbookMarket(marketVO)
                updateMarketPrice()

                addChatMessagesObservers(channel)
            }
        }
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
        chatMessagesPin?.unbind()
        _offerbookListItems.value = emptyList()

        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = channel.chatMessages
        chatMessagesPin =
            chatMessages.addObserver(object : CollectionObserver<BisqEasyOfferbookMessage> {
                override fun add(message: BisqEasyOfferbookMessage) {
                    if (message.hasBisqEasyOffer()) {
                        val offerItemPresentationDto: OfferItemPresentationDto = createOfferListItem(message)
                        val offerItemPresentationModel = OfferItemPresentationModel(offerItemPresentationDto)
                        _offerbookListItems.value += offerItemPresentationModel
                        bisqEasyOfferbookMessages.add(message)
                        log.i { "add offer $offerItemPresentationModel" }
                    }
                }

                override fun remove(message: Any) {
                    if (message is BisqEasyOfferbookMessage && message.hasBisqEasyOffer()) {
                        val item = _offerbookListItems.value.firstOrNull { it.bisqEasyOffer.id == message.bisqEasyOffer.orElse(null)?.id }
                        item?.let {
                            _offerbookListItems.value -= it
                            bisqEasyOfferbookMessages.remove(message)
                            log.i { "Removed offer: $it" }
                        }
                    }
                }

                override fun clear() {
                    _offerbookListItems.value = emptyList()
                    bisqEasyOfferbookMessages.clear()
                }
            })
    }

    private fun fillMarketListItems(): MutableList<MarketListItem> {
        val offerbookMarketItems: MutableList<MarketListItem> = mutableListOf()
        bisqEasyOfferbookChannelService.channels
            .forEach { channel ->
                val marketVO = network.bisq.mobile.domain.data.replicated.common.currency.MarketVO(
                    channel.market.baseCurrencyCode,
                    channel.market.quoteCurrencyCode,
                    channel.market.baseCurrencyName,
                    channel.market.quoteCurrencyName,
                )

                // We convert channel.market to our replicated Market model
                val offerbookMarketItem = MarketListItem(marketVO)

                val market = Mappings.MarketMapping.toBisq2Model(marketVO)
                if (marketPriceService.marketPriceByCurrencyMap.isEmpty() ||
                    marketPriceService.marketPriceByCurrencyMap.containsKey(market)
                ) {
                    offerbookMarketItems.add(offerbookMarketItem)
                    val numOffersObserver = NumOffersObserver(channel, offerbookMarketItem::setNumOffers)
                    numOffersObservers.add(numOffersObserver)
                }
            }
        return offerbookMarketItems
    }

    private fun observeMarketPrice() {
        marketPricePin = marketPriceService.marketPriceByCurrencyMap.addObserver {
            marketPriceService.findMarketPriceQuote(marketPriceService.selectedMarket.get())
            updateMarketPrice()
        }
    }

    private fun updateMarketPrice() {
        if (marketPriceServiceFacade.selectedMarketPriceItem.value != null) {
            val formattedPrice = marketPriceServiceFacade.selectedMarketPriceItem.value!!.formattedPrice
            _selectedOfferbookMarket.value.setFormattedPrice(formattedPrice)
        }
    }

    private fun findBisqEasyOfferbookMessage(offerId: String): Optional<BisqEasyOfferbookMessage> =
        bisqEasyOfferbookMessages.stream()
            .filter { it.hasBisqEasyOffer() }
            .filter { it.bisqEasyOffer.get().id.equals(offerId) }
            .findAny()
}