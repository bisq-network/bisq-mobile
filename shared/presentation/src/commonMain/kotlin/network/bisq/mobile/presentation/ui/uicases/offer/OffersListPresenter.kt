package network.bisq.mobile.presentation.ui.uicases.offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.trade.take_offer.TakeOfferPresenter


class OffersListPresenter(
    mainPresenter: MainPresenter,
    offerbookServiceFacade: OfferbookServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter
) : BasePresenter(mainPresenter) {
    val offerListItems: StateFlow<List<OfferListItemVO>> =
        offerbookServiceFacade.offerListItems

    private val _selectedDirection = MutableStateFlow(DirectionEnum.SELL)
    val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection

    fun takeOffer(offerListItem: OfferListItemVO) {
        takeOfferPresenter.selectOfferToTake(offerListItem)

        if (takeOfferPresenter.showAmountScreen()) {
            rootNavigator.navigate(Routes.TakeOfferTradeAmount.name)
        } else if (takeOfferPresenter.showPaymentMethodsScreen()) {
            rootNavigator.navigate(Routes.TakeOfferPaymentMethod.name)
        } else {
            rootNavigator.navigate(Routes.TakeOfferReviewTrade.name)
        }
    }

    fun chatForOffer(offerListItem: OfferListItemVO) {
        log.i { "chat for offer clicked " }
    }

    fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }
}
