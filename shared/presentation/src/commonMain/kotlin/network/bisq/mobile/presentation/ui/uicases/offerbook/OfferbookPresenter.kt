package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMaxAmount
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMinAmount
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
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
    private val _offerbookListItems: MutableStateFlow<List<OfferItemPresentationModel>> = MutableStateFlow(emptyList())
    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> = _offerbookListItems

    //todo for dev testing its more convenient
    private val _selectedDirection = MutableStateFlow(DirectionEnum.SELL)
    val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection
    private val includeOfferPredicate: MutableStateFlow<(OfferItemPresentationModel) -> Boolean> =
        MutableStateFlow { _: OfferItemPresentationModel -> true }

    val sortedFilteredOffers: StateFlow<List<OfferItemPresentationModel>> =
        combine(
            offersServiceFacade.offerbookListItems,
            selectedDirection,
            includeOfferPredicate
        ) { offers, direction, predicate ->
            offers.filter { it.bisqEasyOffer.direction.mirror == direction } // Use mirrored direction as we are in potential taker role
                .filter(predicate)
                .sortedWith(compareByDescending<OfferItemPresentationModel> { it.bisqEasyOffer.date }
                    .thenBy { it.bisqEasyOffer.id })
        }.stateIn(
            scope = presenterScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation

    private val _showNotEnoughReputationDialog = MutableStateFlow(false)
    val showNotEnoughReputationDialog: StateFlow<Boolean> = _showNotEnoughReputationDialog

    var notEnoughReputationHeadline: String = ""
    var notEnoughReputationMessage: String = ""

    private var selectedOffer: OfferItemPresentationModel? = null

    init {
        collectUI(mainPresenter.languageCode) {
            _offerbookListItems.value = offersServiceFacade.offerbookListItems.value.map {
                it.apply {
                    formattedQuoteAmount = when (it.bisqEasyOffer.amountSpec) {
                        is FixedAmountSpecVO -> {
                            val amountSpec: FixedAmountSpecVO = it.bisqEasyOffer.amountSpec as FixedAmountSpecVO
                            val fiatVO =
                                FiatVOFactory.from(amountSpec.amount, it.bisqEasyOffer.market.quoteCurrencyCode)
                            AmountFormatter.formatAmount(fiatVO, true, true)
                        }

                        is RangeAmountSpecVO -> {
                            val amountSpec: RangeAmountSpecVO = it.bisqEasyOffer.amountSpec as RangeAmountSpecVO
                            val minFiatVO =
                                FiatVOFactory.from(amountSpec.minAmount, it.bisqEasyOffer.market.quoteCurrencyCode)
                            val maxFiatVO =
                                FiatVOFactory.from(amountSpec.maxAmount, it.bisqEasyOffer.market.quoteCurrencyCode)
                            AmountFormatter.formatRangeAmount(minFiatVO, maxFiatVO, true, true)
                        }

                    }
                    formattedPriceSpec = PriceSpecFormatter.getFormattedPriceSpec(it.bisqEasyOffer.priceSpec)
                }
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()

        selectedOffer = null
        includeOfferPredicate.value = { _ -> true } // Reset to trigger update at screen change

        launchUI {
            combine(
                offersServiceFacade.offerbookListItems,
                selectedDirection
            ) { offers, direction ->
                offers to direction // create a new object to enforce to always emits
            }.collect { (_, _) ->
                updateIncludeOfferPredicate()
            }
        }

        updateIncludeOfferPredicate()
    }

    fun onOfferSelected(item: OfferItemPresentationModel) {
        selectedOffer = item
        if (item.isMyOffer) {
            _showDeleteConfirmation.value = true
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
                        val result = offersServiceFacade.deleteOffer(item.offerId).getOrDefault(false)
                        log.d { "delete offer success $result" }
                        if (result) {
                            _showDeleteConfirmation.value = false
                            deselectOffer()
                        } else {
                            log.w { "Failed to delete offer ${item.offerId}" }
                            showSnackbar("Failed to delete offer ${item.offerId}, please try again", true)
                        }
                    }
                }
            }
        }.onFailure {
            log.e(it) { "Failed to delete offer ${selectedOffer?.offerId}" }
            showSnackbar(
                "Unable to delete offer ${selectedOffer?.offerId}",
                true
            )
            deselectOffer()
        }
    }

    fun onDismissDeleteOffer() {
        _showDeleteConfirmation.value = false
        deselectOffer()
    }

    private fun updateIncludeOfferPredicate() {
        launchIO {
            val invalidSellOfferIds = coroutineScope {
                sortedFilteredOffers.value
                    .filter { it.bisqEasyOffer.direction == DirectionEnum.SELL }
                    .map { offer ->
                        async {
                            if (BisqEasyTradeAmountLimits.isSellOfferInvalid(
                                    offer,
                                    true,
                                    marketPriceServiceFacade,
                                    reputationServiceFacade
                                )
                            ) {
                                offer.bisqEasyOffer.id
                            } else {
                                null
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .toSet()
            }
            includeOfferPredicate.value = { item ->
                item.bisqEasyOffer.id !in invalidSellOfferIds
            }
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
                                navigateTo(Routes.TakeOfferPaymentMethod)
                            } else {
                                navigateTo(Routes.TakeOfferReviewTrade)
                            }
                        } else {
                            _showNotEnoughReputationDialog.value = true
                        }
                    } catch (e: Exception) {
                        log.e("canTakeOffer call failed", e)
                    }
                }
            }
        }.onFailure {
            log.e(it) { "Failed to take offer ${selectedOffer?.offerId}" }
            showSnackbar(
                "Unable to take offer ${selectedOffer?.offerId}",
                true
            )
            deselectOffer()
        }
    }

    fun onLearnHowToBuildReputation() {
        _showNotEnoughReputationDialog.value = false
    }

    fun onDismissNotEnoughReputationDialog() {
        _showNotEnoughReputationDialog.value = false
    }

    private suspend fun canTakeOffer(item: OfferItemPresentationModel): Boolean {
        val bisqEasyOffer = item.bisqEasyOffer
        val selectedUserProfile = userProfileServiceFacade.getSelectedUserProfile()
        require(selectedUserProfile != null) { "SelectedUserProfile is null" }
        val requiredReputationScoreForMaxOrFixed =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(
                marketPriceServiceFacade,
                bisqEasyOffer
            )
        require(requiredReputationScoreForMaxOrFixed != null) { "requiredReputationScoreForMaxOrFixedAmount is null" }
        val requiredReputationScoreForMinOrFixed =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
                marketPriceServiceFacade,
                bisqEasyOffer
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

        val userProfileId = if (bisqEasyOffer.direction == DirectionEnum.SELL)
            bisqEasyOffer.makerNetworkId.pubKey.id // Offer maker is seller
        else
            selectedUserProfile.id // I am seller

        val sellersScore: Long = run {
            val reputationScoreResult: Result<ReputationScoreVO> = withContext(IODispatcher) {
                reputationServiceFacade.getReputation(userProfileId)
            }
            reputationScoreResult.exceptionOrNull()?.let { exception ->
                log.w("Exception at reputationServiceFacade.getReputation", exception)
            }
            reputationScoreResult.getOrNull()?.totalScore ?: 0
        }

        val isAmountRangeOffer = bisqEasyOffer.amountSpec is RangeAmountSpecVO

        val canBuyerTakeOffer = sellersScore >= requiredReputationScoreForMinOrFixed
        if (!canBuyerTakeOffer) {
            val link = "hyperlinks.openInBrowser.attention".i18n("https://bisq.wiki/Reputation#How_to_build_reputation")
            if (bisqEasyOffer.direction == DirectionEnum.SELL) {
                // I am as taker the buyer. We check if seller has the required reputation
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
                //  I am as taker the seller. We check if my reputation permits to take the offer
                val learnMore = "mobile.reputation.buildReputation".i18n()
                notEnoughReputationHeadline = "chat.message.takeOffer.seller.insufficientScore.headline".i18n()
                val warningKey =
                    if (isAmountRangeOffer) "chat.message.takeOffer.seller.insufficientScore.rangeAmount.warning"
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
            val market = offersServiceFacade.selectedOfferbookMarket.value.market
            createOfferPresenter.onStartCreateOffer()
            createOfferPresenter.commitMarket(market)
            enableInteractive()
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            enableInteractive()
            log.e(e) { "Failed to create offer" }
            showSnackbar(
                if (isDemo()) "Create offer is disabled in demo mode" else "Cannot create offer at this time, please try again later"
            )
        }
    }
}
