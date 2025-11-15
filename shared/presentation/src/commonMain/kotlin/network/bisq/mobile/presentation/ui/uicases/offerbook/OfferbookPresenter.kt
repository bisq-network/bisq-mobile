package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
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
import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING
import network.bisq.mobile.presentation.ui.navigation.NavRoute
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

    private val _selectedPaymentMethodIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaymentMethodIds: StateFlow<Set<String>> = _selectedPaymentMethodIds.asStateFlow()
    private val _selectedSettlementMethodIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSettlementMethodIds: StateFlow<Set<String>> get() = _selectedSettlementMethodIds.asStateFlow()
    private val _onlyMyOffers = MutableStateFlow(false)
    val onlyMyOffers: StateFlow<Boolean> get() = _onlyMyOffers.asStateFlow()

    private val _sortedFilteredOffers = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    val sortedFilteredOffers: StateFlow<List<OfferItemPresentationModel>> get() = _sortedFilteredOffers.asStateFlow()

    // Baseline available method sets (direction+ignored-user filtered, independent of method selections)
    private val _availablePaymentMethodIds = MutableStateFlow<Set<String>>(emptySet())
    val availablePaymentMethodIds: StateFlow<Set<String>> get() = _availablePaymentMethodIds.asStateFlow()
    private val _availableSettlementMethodIds = MutableStateFlow<Set<String>>(emptySet())
    val availableSettlementMethodIds: StateFlow<Set<String>> get() = _availableSettlementMethodIds.asStateFlow()

    // Presenter-provided UI state for the filter controller
    private val _filterUiState = MutableStateFlow(
        OfferbookFilterUiState(
            payment = emptyList(),
            settlement = emptyList(),
            onlyMyOffers = false,
            hasActiveFilters = false,
        )
    )
    val filterUiState: StateFlow<OfferbookFilterUiState> = _filterUiState.asStateFlow()

    // Track availability deltas and whether user customized filters
    private var prevAvailPayment: Set<String> = emptySet()
    private var prevAvailSettlement: Set<String> = emptySet()
    private var hasManualPaymentFilter: Boolean = false
    private var hasManualSettlementFilter: Boolean = false

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> get() = _showDeleteConfirmation.asStateFlow()

    private val _showNotEnoughReputationDialog = MutableStateFlow(false)
    val showNotEnoughReputationDialog: StateFlow<Boolean> get() = _showNotEnoughReputationDialog.asStateFlow()

    val selectedMarket get() = marketPriceServiceFacade.selectedMarketPriceItem

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    var notEnoughReputationHeadline: String = ""
    var notEnoughReputationMessage: String = ""
    var isReputationWarningForSellerAsTaker: Boolean = false

    private var selectedOffer: OfferItemPresentationModel? = null

    val selectedUserProfile get() = userProfileServiceFacade.selectedUserProfile
    val isLoading get() = offersServiceFacade.isOfferbookLoading


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewAttached() {
        super.onViewAttached()

        selectedOffer = null
        launchIO {
            // pack strongly-typed, use vararg combine -> Array, then map
            combine(
                offersServiceFacade.offerbookListItems,
                selectedDirection,
                offersServiceFacade.selectedOfferbookMarket,
                mainPresenter.languageCode, // included to refresh formatting when language changes
                userProfileServiceFacade.selectedUserProfile,
                selectedPaymentMethodIds,
                selectedSettlementMethodIds,
                onlyMyOffers,
            ) { values: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                OfferbookPresenterInputs(
                    offers = values[0] as List<OfferItemPresentationModel>,
                    direction = values[1] as DirectionEnum,
                    selectedMarket = values[2] as network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket,
                    selectedProfile = values[4] as UserProfileVO?,
                    payments = values[5] as Set<String>,
                    settlements = values[6] as Set<String>,
                    onlyMine = values[7] as Boolean,
                )
            }
                .mapLatest { inp ->
                    val offers = inp.offers
                    val direction = inp.direction
                    val selectedMarket = inp.selectedMarket
                    val selectedProfile = inp.selectedProfile
                    val payments = inp.payments
                    val settlements = inp.settlements
                    val onlyMine = inp.onlyMine

                    log.d { "OfferbookPresenter filtering - Market: ${selectedMarket.market.quoteCurrencyCode}, Dir: $direction, In: ${offers.size}, paySel=${payments.size}, setSel=${settlements.size}, onlyMine=$onlyMine" }

                    val filtered = mutableListOf<OfferItemPresentationModel>()
                    if (selectedProfile == null) return@mapLatest filtered to selectedProfile
                    var directionFilteredCount = 0
                    var ignoredUserFilteredCount = 0
                    var methodFilteredCount = 0
                    var onlyMyFilteredCount = 0

                    // Baseline availability (direction + ignored-user + only-my if enabled), independent of method selections
                    val availablePayments = mutableSetOf<String>()
                    val availableSettlements = mutableSetOf<String>()

                    for (item in offers) {
                        val offerCurrency = item.bisqEasyOffer.market.quoteCurrencyCode
                        val offerDirection = item.bisqEasyOffer.direction.mirror
                        val isIgnoredUser = isOfferFromIgnoredUserCached(item.bisqEasyOffer)

                        log.v { "Offer ${item.offerId} - Currency: $offerCurrency, Direction: $offerDirection, IsIgnored: $isIgnoredUser, isMy=${item.isMyOffer}" }

                        if (offerDirection != direction) {
                            log.v { "Offer ${item.offerId} filtered out (wrong direction: $offerDirection != $direction)" }
                            continue
                        }
                        directionFilteredCount++

                        if (isIgnoredUser) {
                            ignoredUserFilteredCount++
                            log.v { "Offer ${item.offerId} filtered out (ignored user)" }
                            continue
                        }

                        if (onlyMine && !item.isMyOffer) {
                            onlyMyFilteredCount++
                            log.v { "Offer ${item.offerId} filtered out (only my offers enabled)" }
                            continue
                        }

                        // Contribute to baseline availability regardless of current method selections
                        availablePayments.addAll(item.quoteSidePaymentMethods)
                        availableSettlements.addAll(item.baseSidePaymentMethods)

                        // Method filter: empty selections mean "no filter" unless the user manually customized this filter
                        val paymentOk = if (payments.isEmpty() && !hasManualPaymentFilter) true else item.quoteSidePaymentMethods.any { it in payments }
                        val settlementOk = if (settlements.isEmpty() && !hasManualSettlementFilter) true else item.baseSidePaymentMethods.any { it in settlements }
                        if (!paymentOk || !settlementOk) {
                            methodFilteredCount++
                            log.v { "Offer ${item.offerId} filtered out (methods) payOk=$paymentOk setOk=$settlementOk" }
                            continue
                        }

                        filtered += item
                        log.v { "Offer ${item.offerId} included - Currency: $offerCurrency, Amount: ${item.formattedQuoteAmount}" }
                    }


                    // Publish baseline availability independent of current method selections
                    _availablePaymentMethodIds.value = availablePayments
                    _availableSettlementMethodIds.value = availableSettlements

                    log.d { "OfferbookPresenter filtering results - Market: ${selectedMarket.market.quoteCurrencyCode}, Dir matches: $directionFilteredCount, Ignored: $ignoredUserFilteredCount, OnlyMy: $onlyMyFilteredCount, Methods: $methodFilteredCount, Final: ${filtered.size}" }
                    filtered to selectedProfile
                }
                .collectLatest { (filtered, selectedProfile) ->
                    if (selectedProfile != null) {
                        val processed = processAllOffers(filtered, selectedProfile)
                        val sorted = processed.sortedWith(
                            compareByDescending<OfferItemPresentationModel> { it.bisqEasyOffer.date }.thenBy { it.bisqEasyOffer.id })
                        _sortedFilteredOffers.value = sorted
                        log.d { "OfferbookPresenter final result - ${sorted.size} offers displayed for market" }
                    }
                }
        }

        // Derive and publish filter UI state from available + selected sets
        launchIO {
            combine(
                availablePaymentMethodIds,
                availableSettlementMethodIds,
                selectedPaymentMethodIds,
                selectedSettlementMethodIds,
                onlyMyOffers,
            ) { payAvail, setAvail, paySel, setSel, onlyMine ->
                val paymentUi = payAvail.toList().sorted().map { id ->
                    MethodIconState(
                        id = id,
                        label = humanizePaymentId(id),
                        iconPath = paymentIconPath(id),
                        selected = id in paySel
                    )
                }
                val settlementUi = setAvail.toList().sorted().map { id ->
                    val label = settlementLabelFor(id)
                    MethodIconState(
                        id = id,
                        label = label,
                        iconPath = settlementIconPath(id),
                        selected = id in setSel
                    )
                }
                val hasActive = onlyMine || paymentUi.any { !it.selected } || settlementUi.any { !it.selected }
                OfferbookFilterUiState(
                    payment = paymentUi,
                    settlement = settlementUi,
                    onlyMyOffers = onlyMine,
                    hasActiveFilters = hasActive,
                )
            }.collectLatest { ui ->
                _filterUiState.value = ui
            }
        }

        // Auto-manage default selections and availability changes
        launchIO {
            availablePaymentMethodIds.collectLatest { avail ->
                val current = _selectedPaymentMethodIds.value
                val newlyAdded = avail - prevAvailPayment
                val newSelection = (current intersect avail) + (if (hasManualPaymentFilter) emptySet() else newlyAdded)
                val finalSelection = if (current.isEmpty()) avail else newSelection
                if (finalSelection != current) {
                    _selectedPaymentMethodIds.value = finalSelection
                }
                prevAvailPayment = avail
            }
        }
        launchIO {
            availableSettlementMethodIds.collectLatest { avail ->
                val current = _selectedSettlementMethodIds.value
                val newlyAdded = avail - prevAvailSettlement
                val newSelection = (current intersect avail) + (if (hasManualSettlementFilter) emptySet() else newlyAdded)
                val finalSelection = if (current.isEmpty()) avail else newSelection
                if (finalSelection != current) {
                    _selectedSettlementMethodIds.value = finalSelection
                }
                prevAvailSettlement = avail
            }
        }

    }

    private suspend fun processAllOffers(
        offers: List<OfferItemPresentationModel>,
        userProfile: UserProfileVO,
    ): List<OfferItemPresentationModel> = withContext(Dispatchers.IO) {
        offers.map { offer -> processOffer(offer, userProfile) }
    }

    private suspend fun processOffer(item: OfferItemPresentationModel, userProfile: UserProfileVO): OfferItemPresentationModel {
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
        }

        val formattedPrice = PriceSpecFormatter.getFormattedPriceSpec(offer.priceSpec)

        val isInvalid = if (offer.direction == DirectionEnum.BUY) {
            BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                item = item,
                useCache = true,
                marketPriceServiceFacade = marketPriceServiceFacade,
                reputationServiceFacade = reputationServiceFacade,
                userProfileId = userProfile.id
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

    private fun humanizePaymentId(id: String): String {
        val (name, missing) = network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod(id)
        if (!missing) return name
        val acronyms = setOf("SEPA", "SWIFT", "ACH", "UPI", "PIX", "ZELLE", "F2F")
        return id.split('_', '-').joinToString(" ") { part ->
            val up = part.uppercase()
            if (up in acronyms) up else part.lowercase().replaceFirstChar { it.titlecase() }
        }
    }

    private fun settlementLabelFor(id: String): String = when (id.uppercase()) {
        "BTC", "MAIN_CHAIN", "ONCHAIN", "ON_CHAIN" -> "mobile.settlement.bitcoin".i18n()
        "LIGHTNING", "LN" -> "mobile.settlement.lightning".i18n()
        else -> id
    }


    fun onConfirmedDeleteOffer() {
        val selectedOffer = this.selectedOffer
        if (selectedOffer == null) {
            _showDeleteConfirmation.value = false
            showSnackbar("mobile.bisqEasy.offerbook.failedToDeleteOffer".i18n(EMPTY_STRING), true)
            return
        }
        runCatching {
            _showDeleteConfirmation.value = false
            require(selectedOffer.isMyOffer)
            launchUI {
                showLoading()
                withContext(Dispatchers.IO) {
                    val result = offersServiceFacade.deleteOffer(selectedOffer.offerId)
                        .getOrDefault(false)
                    log.d { "delete offer success $result" }
                    hideLoading()
                    if (result) {
                        deselectOffer()
                    } else {
                        log.w { "Failed to delete offer ${selectedOffer.offerId}" }
                        showSnackbar(
                            "mobile.bisqEasy.offerbook.failedToDeleteOffer".i18n(
                                selectedOffer.offerId
                            ), true
                        )
                    }
                }
            }
        }.onFailure {
            hideLoading()
            log.e(it) { "Failed to delete offer ${selectedOffer.offerId}" }
            showSnackbar(
                "mobile.bisqEasy.offerbook.unableToDeleteOffer".i18n(selectedOffer.offerId),
                true
            )
            deselectOffer()
        }
    }

    fun onDismissDeleteOffer() {
        _showDeleteConfirmation.value = false
        deselectOffer()
    }

    private fun takeOffer() {
        runCatching {
            selectedOffer?.let { item ->
                require(!item.isMyOffer)
                val selectedProfile = selectedUserProfile.value
                require(selectedProfile != null)
                launchUI {
                    try {
                        if (canTakeOffer(item, selectedProfile)) {
                            takeOfferPresenter.selectOfferToTake(item)
                            if (takeOfferPresenter.showAmountScreen()) {
                                navigateTo(NavRoute.TakeOfferTradeAmount)
                            } else if (takeOfferPresenter.showPaymentMethodsScreen()) {
                                navigateTo(NavRoute.TakeOfferPaymentMethod)
                            } else if (takeOfferPresenter.showSettlementMethodsScreen()) {
                                navigateTo(NavRoute.TakeOfferSettlementMethod)
                            } else {
                                navigateTo(NavRoute.TakeOfferReviewTrade)
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

    private suspend fun canTakeOffer(item: OfferItemPresentationModel, userProfile: UserProfileVO): Boolean {
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
            userProfile.id // I am seller (taker selling to maker who wants to buy)
        }

        val reputationResult: Result<ReputationScoreVO> = withContext(Dispatchers.IO) {
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
            val link = "hyperlinks.openInBrowser.attention".i18n(BisqLinks.REPUTATION_WIKI_URL)
            val takersDirection = bisqEasyOffer.direction.mirror
            isReputationWarningForSellerAsTaker = takersDirection == DirectionEnum.SELL
            if (takersDirection == DirectionEnum.BUY) {
                // SELL offer: Maker wants to sell Bitcoin, so they are the seller
                // Taker (me) wants to buy Bitcoin - checking if seller has enough reputation
                val learnMore = "mobile.reputation.learnMoreAtWiki".i18n()
                notEnoughReputationHeadline = "chat.message.takeOffer.buyer.invalidOffer.headline".i18n()
                val warningKey = if (isAmountRangeOffer) "chat.message.takeOffer.buyer.invalidOffer.rangeAmount.text"
                else "chat.message.takeOffer.buyer.invalidOffer.fixedAmount.text"

                notEnoughReputationMessage = warningKey.i18n(
                    sellersScore,
                    if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                    if (isAmountRangeOffer) minFiatAmount else maxFiatAmount
                ) + "\n\n" + learnMore + "\n\n" + link
            } else {
                // BUY offer: Maker wants to buy Bitcoin, so taker becomes the seller
                // Taker (me) wants to sell Bitcoin - checking if I have enough reputation
                notEnoughReputationHeadline = "chat.message.takeOffer.seller.insufficientScore.headline".i18n()
                val warningKey = if (isAmountRangeOffer) "chat.message.takeOffer.seller.insufficientScore.rangeAmount.warning"
                else "chat.message.takeOffer.seller.insufficientScore.fixedAmount.warning"
                notEnoughReputationMessage = warningKey.i18n(
                    sellersScore,
                    if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                    if (isAmountRangeOffer) minFiatAmount else maxFiatAmount
                ) + "\n\n" + "mobile.reputation.warning.navigateToReputation".i18n()
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

    fun setOnlyMyOffers(enabled: Boolean) {
        _onlyMyOffers.value = enabled
    }

    fun setSelectedPaymentMethodIds(ids: Set<String>) {
        val avail = _availablePaymentMethodIds.value
        val clamped = ids intersect avail
        hasManualPaymentFilter = clamped != avail
        _selectedPaymentMethodIds.value = clamped
    }

    fun setSelectedSettlementMethodIds(ids: Set<String>) {
        val avail = _availableSettlementMethodIds.value
        val clamped = ids intersect avail
        hasManualSettlementFilter = clamped != avail
        _selectedSettlementMethodIds.value = clamped
    }

    fun togglePaymentMethod(id: String) {
        val avail = _availablePaymentMethodIds.value
        if (id !in avail) return
        val current = _selectedPaymentMethodIds.value
        val next = if (id in current) current - id else current + id
        hasManualPaymentFilter = true
        _selectedPaymentMethodIds.value = next
    }

    fun toggleSettlementMethod(id: String) {
        val avail = _availableSettlementMethodIds.value
        if (id !in avail) return
        val current = _selectedSettlementMethodIds.value
        val next = if (id in current) current - id else current + id
        hasManualSettlementFilter = true
        _selectedSettlementMethodIds.value = next
    }

    fun clearAllFilters() {
        hasManualPaymentFilter = false
        hasManualSettlementFilter = false
        _selectedPaymentMethodIds.value = _availablePaymentMethodIds.value
        _selectedSettlementMethodIds.value = _availableSettlementMethodIds.value
        _onlyMyOffers.value = false
    }

    fun setPaymentSelection(ids: Set<String>) {
        setSelectedPaymentMethodIds(ids)
    }

    fun setSettlementSelection(ids: Set<String>) {
        setSelectedSettlementMethodIds(ids)
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
            navigateTo(NavRoute.CreateOfferDirection)
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
                val selectedProfile = selectedUserProfile.value
                if (selectedProfile == null) {
                    throw IllegalStateException("selectedUserProfile is null")
                }
                // Set up the dialog content
                setupReputationDialogContent(item, selectedProfile)

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

    fun onNavigateToReputation() {
        navigateTo(NavRoute.Reputation)
        _showNotEnoughReputationDialog.value = false
    }

    fun onOpenReputationWiki() {
        _showNotEnoughReputationDialog.value = false
        navigateToUrl(BisqLinks.BUILD_REPUTATION_WIKI_URL)
    }

    private suspend fun setupReputationDialogContent(item: OfferItemPresentationModel, userProfile: UserProfileVO) {
        canTakeOffer(item, userProfile)
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
