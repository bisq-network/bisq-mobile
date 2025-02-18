package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    var selectedOffer: OfferItemPresentationModel? = null

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
                    backgroundScope.launch {
                        offersServiceFacade.deleteOffer(item.offerId)
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
            showSnackbar("Unable to ${if (selectedOffer?.isMyOffer == true) "delete" else "take"} offer ${selectedOffer?.offerId}, please try again", true)
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
        val market =offersServiceFacade.selectedOfferbookMarket.value.market
        createOfferPresenter.onStartCreateOffer()
        createOfferPresenter.commitMarket(market)
        navigateTo(Routes.CreateOfferDirection)
    }
}
