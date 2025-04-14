package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter


class OfferbookPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {
    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> = offersServiceFacade.offerbookListItems

    //todo for dev testing its more convenient
    private val _selectedDirection = MutableStateFlow(DirectionEnum.BUY)
    val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation
    private var selectedOffer: OfferItemPresentationModel? = null

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
