package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMaxAmount
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMinAmount
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter

class OfferbookPresenter(
    private val mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade
) : BasePresenter(mainPresenter) {

    private val _selectedDirection = MutableStateFlow(DirectionEnum.BUY)
    val selectedDirection: StateFlow<DirectionEnum> get() = _selectedDirection.asStateFlow()

    private val _sortedFilteredOffers = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    val sortedFilteredOffers: StateFlow<List<OfferItemPresentationModel>> get() = _sortedFilteredOffers.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> get() = _showDeleteConfirmation.asStateFlow()

    private val _showNotEnoughReputationDialog = MutableStateFlow(false)
    val showNotEnoughReputationDialog: StateFlow<Boolean> get() = _showNotEnoughReputationDialog.asStateFlow()

    val selectedMarket get() = marketPriceServiceFacade.selectedMarketPriceItem

    var notEnoughReputationHeadline: String = ""
    var notEnoughReputationMessage: String = ""

    private var selectedOffer: OfferItemPresentationModel? = null

    lateinit var selectedUserProfile: UserProfileVO

    private val _avatarMap: MutableStateFlow<Map<String, PlatformImage?>> = MutableStateFlow(
        emptyMap()
    )
    val avatarMap: StateFlow<Map<String, PlatformImage?>> get() = _avatarMap.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewAttached() {
        super.onViewAttached()

        selectedOffer = null

        launchIO {
            userProfileServiceFacade.getSelectedUserProfile()?.let { selectedUserProfile = it }
                ?: run {
                    log.w { "No selected user profile; offer list skipped" }
                    return@launchIO
                }

            combine(
                offersServiceFacade.offerbookListItems,
                selectedDirection,
                offersServiceFacade.selectedOfferbookMarket,
                mainPresenter.languageCode
            ) { offers, direction, selectedMarket, _ ->
                Triple(offers, direction, selectedMarket)
            }
            .mapLatest { (offers, direction, selectedMarket) ->
                log.d { "OfferbookPresenter filtering - Selected market: ${selectedMarket.market.quoteCurrencyCode}, Direction: $direction, Input offers: ${offers.size}" }

                val filtered = mutableListOf<OfferItemPresentationModel>()
                var directionFilteredCount = 0
                var ignoredUserFilteredCount = 0

                for (item in offers) {
                    val offerCurrency = item.bisqEasyOffer.market.quoteCurrencyCode
                    val offerDirection = item.bisqEasyOffer.direction.mirror
                    val isIgnoredUser = isOfferFromIgnoredUserCached(item.bisqEasyOffer)

                    log.v { "Offer ${item.offerId} - Currency: $offerCurrency, Direction: $offerDirection, IsIgnored: $isIgnoredUser" }

                    if (offerDirection == direction) {
                        directionFilteredCount++
                        if (!isIgnoredUser) {
                            filtered += item
                            log.v { "Offer ${item.offerId} included - Currency: $offerCurrency, Amount: ${item.formattedQuoteAmount}" }
                        } else {
                            ignoredUserFilteredCount++
                            log.v { "Offer ${item.offerId} filtered out (ignored user)" }
                        }
                    } else {
                        log.v { "Offer ${item.offerId} filtered out (wrong direction: $offerDirection != $direction)" }
                    }
                }

                log.d { "OfferbookPresenter filtering results - Market: ${selectedMarket.market.quoteCurrencyCode}, Direction matches: $directionFilteredCount, Ignored users filtered: $ignoredUserFilteredCount, Final count: ${filtered.size}" }
                filtered
            }
            .collectLatest { filtered ->
                val processed = processAllOffers(filtered)
                val sorted = processed.sortedWith(
                    compareByDescending<OfferItemPresentationModel> { it.bisqEasyOffer.date }.thenBy { it.bisqEasyOffer.id })
                _sortedFilteredOffers.value = sorted
                log.d { "OfferbookPresenter final result - ${sorted.size} offers displayed for market" }
            }
        }
    }

    override fun onViewUnattaching() {
        _avatarMap.update { emptyMap() }
        super.onViewUnattaching()
    }

    private suspend fun processAllOffers(
        offers: List<OfferItemPresentationModel>
    ): List<OfferItemPresentationModel> = withContext(IODispatcher) {
        offers.map { offer -> processOffer(offer) }
    }

    private suspend fun processOffer(item: OfferItemPresentationModel): OfferItemPresentationModel {
        val offer = item.bisqEasyOffer

        // todo: Reformatting should ideally only happen with language change
        val formattedQuoteAmount = when (val amountSpec = offer.amountSpec) {
            is FixedAmountSpecVO -> {
                val fiatVO = FiatVOFactory.from(amountSpec.amount, offer.market.quoteCurrencyCode)
                AmountFormatter.formatAmount(fiatVO, true, true)
            }

            is RangeAmountSpecVO -> {
                val minFiatVO = FiatVOFactory.from(
                    amountSpec.minAmount, offer.market.quoteCurrencyCode
                )
                val maxFiatVO = FiatVOFactory.from(
                    amountSpec.maxAmount, offer.market.quoteCurrencyCode
                )
                AmountFormatter.formatRangeAmount(minFiatVO, maxFiatVO, true, true)
            }

            else -> ""
        }

        val formattedPrice = PriceSpecFormatter.getFormattedPriceSpec(offer.priceSpec)

        val isInvalid = if (offer.direction == DirectionEnum.BUY) {
            BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                item = item,
                useCache = true,
                marketPriceServiceFacade = marketPriceServiceFacade,
                reputationServiceFacade = reputationServiceFacade,
                userProfileId = selectedUserProfile.id
            )
        } else false

        // Not doing copyWith of item to assign these properties.
        // Because `OfferItemPresentationModel` class has StateFlow props
        // and so creating a new object of it, breaks the flow listeners
        withContext(Dispatchers.Main) {
            item.formattedQuoteAmount = formattedQuoteAmount
            item.formattedPriceSpec = formattedPrice
            item.isInvalidDueToReputation = isInvalid
        }

        ensureAvatarLoaded(item.makersUserProfile)

        return item
    }

    fun onOfferSelected(item: OfferItemPresentationModel) {
        selectedOffer = item
        if (item.isMyOffer) {
            _showDeleteConfirmation.value = true
        } else if (item.isInvalidDueToReputation) {
            showReputationRequirementInfo(item)
        } else {
            takeOffer()
        }
    }

    fun onConfirmedDeleteOffer() {
        runCatching {
            selectedOffer?.let { item ->
                require(item.isMyOffer)
                launchUI {
                    withContext(IODispatcher) {
                        val result = offersServiceFacade.deleteOffer(item.offerId)
                            .getOrDefault(false)
                        log.d { "delete offer success $result" }
                        if (result) {
                            _showDeleteConfirmation.value = false
                            deselectOffer()
                        } else {
                            log.w { "Failed to delete offer ${item.offerId}" }
                            showSnackbar("mobile.bisqEasy.offerbook.failedToDeleteOffer".i18n(item.offerId), true)
                        }
                    }
                }
            }
        }.onFailure {
            log.e(it) { "Failed to delete offer ${selectedOffer?.offerId}" }
            showSnackbar(
                "mobile.bisqEasy.offerbook.unableToDeleteOffer".i18n(selectedOffer?.offerId ?: ""),
                true
            )
            deselectOffer()
        }
    }

    fun onDismissDeleteOffer() {
        _showDeleteConfirmation.value = false
        deselectOffer()
    }

    private suspend fun ensureAvatarLoaded(userProfile: UserProfileVO) = withContext(IODispatcher) {
        val nym = userProfile.nym
        if (_avatarMap.value[nym] == null) {
            val image = userProfileServiceFacade.getUserAvatar(userProfile)
            _avatarMap.update { it + (nym to image) }
        }
    }

    private fun takeOffer() {
        runCatching {
            selectedOffer?.let { item ->
                require(!item.isMyOffer)
                launchUI {
                    try {
                        if (canTakeOffer(item)) {
                            takeOfferPresenter.selectOfferToTake(item)
                            if (takeOfferPresenter.showAmountScreen()) {
                                navigateTo(Routes.TakeOfferTradeAmount)
                            } else if (takeOfferPresenter.showPaymentMethodsScreen()) {
                                navigateTo(Routes.TakeOfferQuoteSidePaymentMethod)
                            } else if (takeOfferPresenter.showSettlementMethodsScreen()) {
                                navigateTo(Routes.TakeOfferBaseSidePaymentMethod)
                            } else {
                                navigateTo(Routes.TakeOfferReviewTrade)
                            }
                        } else {
                            showReputationRequirementInfo(item)
                        }
                    } catch (e: Exception) {
                        log.e("canTakeOffer call failed", e)
                    }
                }
            }
        }.onFailure {
            log.e(it) { "Failed to take offer ${selectedOffer?.offerId}" }
            showSnackbar(
                "mobile.bisqEasy.offerbook.unableToTakeOffer".i18n(selectedOffer?.offerId ?: ""),
                true
            )
            deselectOffer()
        }
    }

    private suspend fun canTakeOffer(item: OfferItemPresentationModel): Boolean {
        val bisqEasyOffer = item.bisqEasyOffer
        val requiredReputationScoreForMaxOrFixed = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(
            marketPriceServiceFacade, bisqEasyOffer
        )
        require(requiredReputationScoreForMaxOrFixed != null) { "requiredReputationScoreForMaxOrFixedAmount is null" }
        val requiredReputationScoreForMinOrFixed = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
            marketPriceServiceFacade, bisqEasyOffer
        )
        require(requiredReputationScoreForMinOrFixed != null) { "requiredReputationScoreForMinAmount is null" }

        val market = bisqEasyOffer.market
        val quoteCurrencyCode = market.quoteCurrencyCode
        val minFiatAmount: String = AmountFormatter.formatAmount(
            FiatVOFactory.from(bisqEasyOffer.getFixedOrMinAmount(), quoteCurrencyCode),
            useLowPrecision = true,
            withCode = true
        )
        val maxFiatAmount: String = AmountFormatter.formatAmount(
            FiatVOFactory.from(bisqEasyOffer.getFixedOrMaxAmount(), quoteCurrencyCode),
            useLowPrecision = true,
            withCode = true
        )

        // For BUY offers: The maker wants to buy Bitcoin, so the taker (me) becomes the seller
        // For SELL offers: The maker wants to sell Bitcoin, so the maker becomes the seller
        val userProfileId = if (bisqEasyOffer.direction == DirectionEnum.SELL) {
            bisqEasyOffer.makerNetworkId.pubKey.id // Offer maker is seller (wants to sell Bitcoin)
        } else {
            selectedUserProfile.id // I am seller (taker selling to maker who wants to buy)
        }

        val reputationResult: Result<ReputationScoreVO> = withContext(IODispatcher) {
            reputationServiceFacade.getReputation(userProfileId)
        }

        val sellersScore: Long = reputationResult.getOrNull()?.totalScore ?: 0
        val isReputationNotCached = reputationResult.exceptionOrNull()?.message?.contains("not cached yet") == true

        reputationResult.exceptionOrNull()?.let { exception ->
            log.w("Exception at reputationServiceFacade.getReputation", exception)
            if (isReputationNotCached) {
                log.i { "Reputation not cached yet for user $userProfileId, allowing offer to be taken" }
            }
        }

        val isAmountRangeOffer = bisqEasyOffer.amountSpec is RangeAmountSpecVO

        // val canBuyerTakeOffer = isReputationNotCached || sellersScore >= requiredReputationScoreForMinOrFixed
        val canBuyerTakeOffer = sellersScore >= requiredReputationScoreForMinOrFixed
        if (!canBuyerTakeOffer) {
            val link = "hyperlinks.openInBrowser.attention".i18n(BisqLinks.REPUTATION_BUILD_WIKI_URL)
            if (bisqEasyOffer.direction == DirectionEnum.SELL) {
                // SELL offer: Maker wants to sell Bitcoin, so they are the seller
                // Taker (me) wants to buy Bitcoin - checking if seller has enough reputation
                val learnMore = "mobile.reputation.learnMore".i18n()
                notEnoughReputationHeadline = "chat.message.takeOffer.buyer.invalidOffer.headline".i18n()
                val warningKey = if (isAmountRangeOffer) "chat.message.takeOffer.buyer.invalidOffer.rangeAmount.text"
                else "chat.message.takeOffer.buyer.invalidOffer.fixedAmount.text"
                notEnoughReputationMessage = warningKey.i18n(
                    sellersScore,
                    if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                    if (isAmountRangeOffer) minFiatAmount else maxFiatAmount
                ) + "\n\n" + learnMore + "\n\n\n" + link
            } else {
                // BUY offer: Maker wants to buy Bitcoin, so taker becomes the seller
                // Taker (me) wants to sell Bitcoin - checking if I have enough reputation
                val learnMore = "mobile.reputation.buildReputation".i18n()
                notEnoughReputationHeadline = "chat.message.takeOffer.seller.insufficientScore.headline".i18n()
                val warningKey = if (isAmountRangeOffer) "chat.message.takeOffer.seller.insufficientScore.rangeAmount.warning"
                else "chat.message.takeOffer.seller.insufficientScore.fixedAmount.warning"
                notEnoughReputationMessage = warningKey.i18n(
                    sellersScore,
                    if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                    if (isAmountRangeOffer) minFiatAmount else maxFiatAmount
                ) + "\n\n" + learnMore + "\n\n\n" + link
            }
        }

        return canBuyerTakeOffer
    }

    private fun deselectOffer() {
        selectedOffer = null
    }

    fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }

    fun createOffer() {
        disableInteractive()
        try {
            val selectedMarket = offersServiceFacade.selectedOfferbookMarket.value.market
            createOfferPresenter.onStartCreateOffer()
            
            // Check if a market is already selected (not EMPTY)
            val hasValidMarket = selectedMarket.baseCurrencyCode.isNotEmpty() && selectedMarket.quoteCurrencyCode.isNotEmpty()
            
            if (hasValidMarket) {
                // Use the already selected market
                createOfferPresenter.commitMarket(selectedMarket)
                createOfferPresenter.skipCurrency = true
            } else {
                // No market selected, go to market selection
                createOfferPresenter.skipCurrency = false
            }
            
            enableInteractive()
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            enableInteractive()
            log.e(e) { "Failed to create offer" }
            showSnackbar(
                if (isDemo()) "mobile.bisqEasy.offerbook.createOfferDisabledInDemoMode".i18n() else "mobile.bisqEasy.offerbook.cannotCreateOffer".i18n()
            )
        }
    }

    fun showReputationRequirementInfo(item: OfferItemPresentationModel) {
        launchUI {
            try {
                // Set up the dialog content
                setupReputationDialogContent(item)

                // Show the dialog
                _showNotEnoughReputationDialog.value = true
            } catch (e: Exception) {
                log.e("showReputationRequirementInfo call failed", e)
            }
        }
    }

    fun onDismissNotEnoughReputationDialog() {
        _showNotEnoughReputationDialog.value = false
    }

    fun onLearnHowToBuildReputation() {
        _showNotEnoughReputationDialog.value = false
        navigateToUrl(BisqLinks.REPUTATION_BUILD_WIKI_URL)
    }

    private suspend fun setupReputationDialogContent(item: OfferItemPresentationModel) {
        canTakeOffer(item)
    }

    private suspend fun isOfferFromIgnoredUser(offer: BisqEasyOfferVO): Boolean {
        val makerUserProfileId = offer.makerNetworkId.pubKey.id
        return try {
            val isIgnored = userProfileServiceFacade.isUserIgnored(makerUserProfileId)
            if (isIgnored) {
                log.v { "Offer ${offer.id} from ignored user $makerUserProfileId" }
            }
            isIgnored
        } catch (e: Exception) {
            log.w("isUserIgnored failed for $makerUserProfileId", e)
            false
        }
    }

    /**
     * Fast, non-suspending check for ignored users using cached data.
     * This method is safe to call from hot paths like offer filtering.
     */
    private fun isOfferFromIgnoredUserCached(offer: BisqEasyOfferVO): Boolean {
        val makerUserProfileId = offer.makerNetworkId.pubKey.id
        return try {
            // Use cached check for hot path performance
            val isIgnored = (userProfileServiceFacade as? network.bisq.mobile.client.service.user_profile.ClientUserProfileServiceFacade)
                ?.isUserIgnoredCached(makerUserProfileId) ?: false

            if (isIgnored) {
                log.v { "Offer ${offer.id} from ignored user $makerUserProfileId (cached)" }
            }
            isIgnored
        } catch (e: Exception) {
            log.w("isUserIgnoredCached failed for $makerUserProfileId", e)
            false
        }
    }
}
