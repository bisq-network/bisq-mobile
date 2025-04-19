package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.FixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceFormatter
import network.bisq.mobile.domain.formatters.PriceSpecFormatter
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter


class OfferbookPresenter(
    private val mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {
    var _offerbookListItems: MutableStateFlow<List<OfferItemPresentationModel>> = MutableStateFlow(emptyList())
    var offerbookListItems: StateFlow<List<OfferItemPresentationModel>> = _offerbookListItems

    //todo for dev testing its more convenient
    private val _selectedDirection = MutableStateFlow(DirectionEnum.BUY)
    val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation
    private var selectedOffer: OfferItemPresentationModel? = null

    init {
        presenterScope.launch {
            mainPresenter.languageCode.collect {
                _offerbookListItems.value = offersServiceFacade.offerbookListItems.value.map {
                    it.apply {
                        formattedQuoteAmount = when (it.bisqEasyOffer.amountSpec) {
                            is FixedAmountSpecVO -> {
                                val amountSpec: FixedAmountSpecVO = it.bisqEasyOffer.amountSpec as FixedAmountSpecVO
                                val fiatVO =
                                    FiatVOFactory.from(amountSpec.amount, it.bisqEasyOffer.market.quoteCurrencyCode)
                                AmountFormatter.formatAmount(fiatVO)
                            }

                            is RangeAmountSpecVO -> {
                                val amountSpec: RangeAmountSpecVO = it.bisqEasyOffer.amountSpec as RangeAmountSpecVO
                                val minFiatVO = FiatVOFactory.from(amountSpec.minAmount, it.bisqEasyOffer.market.quoteCurrencyCode)
                                val maxFiatVO = FiatVOFactory.from(amountSpec.maxAmount, it.bisqEasyOffer.market.quoteCurrencyCode)
                                AmountFormatter.formatRangeAmount(minFiatVO, maxFiatVO, true, true)
                            }

                        }
                        formattedPriceSpec = PriceSpecFormatter.getFormattedPriceSpec(it.bisqEasyOffer.priceSpec)
                    }
                }
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        selectedOffer = null
    }

    fun onSelectOffer(item: OfferItemPresentationModel) {
        selectedOffer = item
        if (item.isMyOffer) {
            _showDeleteConfirmation.value = true
        } else {
            proceedWithOfferAction()
        }
    }

    fun proceedWithOfferAction() {
        runCatching {
            selectedOffer?.let { item ->
                if (item.isMyOffer) {
                    presenterScope.launch {
                        withContext(IODispatcher) {
                            offersServiceFacade.deleteOffer(item.offerId)
                        }
                        deselectOffer()
                    }
                } else {
                    takeOfferPresenter.selectOfferToTake(item)
                    if (takeOfferPresenter.showAmountScreen()) {
                        navigateTo(Routes.TakeOfferTradeAmount)
                    } else if (takeOfferPresenter.showPaymentMethodsScreen()) {
                        navigateTo(Routes.TakeOfferPaymentMethod)
                    } else {
                        navigateTo(Routes.TakeOfferReviewTrade)
                    }
                }
            }
        }.onFailure {
            log.e(it) { "Failed to ${if (selectedOffer?.isMyOffer == true) "delete" else "take"} offer" }
            showSnackbar(
                "Unable to ${if (selectedOffer?.isMyOffer == true) "delete" else "take"} offer ${selectedOffer?.offerId}, please try again",
                true
            )
            deselectOffer()
        }
    }

    fun onCancelDelete() {
        deselectOffer()
    }

    private fun deselectOffer() {
        selectedOffer = null
        _showDeleteConfirmation.value = false
    }

    fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }

    fun createOffer() {
        try {
            val market = offersServiceFacade.selectedOfferbookMarket.value.market
            createOfferPresenter.onStartCreateOffer()
            createOfferPresenter.commitMarket(market)
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer" }
            showSnackbar(
                if (isDemo()) "Create offer is disabled in demo mode" else "Cannot create offer at this time, please try again later"
            )
        }
    }
}
