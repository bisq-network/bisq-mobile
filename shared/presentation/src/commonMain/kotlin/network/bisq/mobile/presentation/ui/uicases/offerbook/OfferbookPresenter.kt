package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter


class OfferbookPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter
) : BasePresenter(mainPresenter) {
    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> = offersServiceFacade.offerbookListItems

    //todo for dev testing its more convenient
    private val _selectedDirection = MutableStateFlow(DirectionEnum.BUY)
    val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection

    fun onSelectOffer(item: OfferItemPresentationModel) {
        runCatching {
            if (item.isMyOffer) {
                //todo show dialogue if user really want to delete their offer
                backgroundScope.launch { offersServiceFacade.deleteOffer(item.offerId) }
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
        }.onFailure {
            // TODO show error to users
            log.e(it) { "Failed to select offer" }
        }
    }

    fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }
}
