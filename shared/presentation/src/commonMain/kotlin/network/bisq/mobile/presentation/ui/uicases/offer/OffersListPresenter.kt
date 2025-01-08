package network.bisq.mobile.presentation.ui.uicases.offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.trade.take_offer.TakeOfferPresenter


class OffersListPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter), IOffersListPresenter {
    override val offerListItems: StateFlow<List<OfferListItemVO>> =
        offerbookServiceFacade.offerListItems

    private val _selectedDirection = MutableStateFlow(DirectionEnum.SELL)
    override val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection

    override fun takeOffer(offer: OfferListItemVO) {
        takeOfferPresenter.selectOfferToTake(offer)

        if (takeOfferPresenter.showAmountScreen()) {
            rootNavigator.navigate(Routes.TakeOfferTradeAmount.name)
        } else if (takeOfferPresenter.showPaymentMethodsScreen()) {
            rootNavigator.navigate(Routes.TakeOfferPaymentMethod.name)
        } else {
            rootNavigator.navigate(Routes.TakeOfferReviewTrade.name)
        }
    }

    override fun createOffer() {
        createOfferPresenter.onStartCreateOffer(offerbookServiceFacade.selectedOfferbookMarket.value.market)
        rootNavigator.navigate(Routes.CreateOfferDirection.name)
    }

    override fun chatForOffer(offer: OfferListItemVO) {
        log.i { "chat for offer clicked " }
    }

    override fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }
}
