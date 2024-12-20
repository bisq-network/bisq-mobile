package network.bisq.mobile.presentation.ui.uicases.offers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.offer.IOffersListPresenter


open class OffersListPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), IOffersListPresenter {
    override val offerListItems: StateFlow<List<OfferListItemVO>> = offerbookServiceFacade.offerListItems

    private val _selectedDirection = MutableStateFlow(DirectionEnum.SELL)
    override val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }

    override fun takeOffer(offer: OfferListItemVO) {
        log.i { "take offer clicked " }
        //todo show take offer screen
        rootNavigator.navigate(Routes.TakeOfferTradeAmount.name)
    }

    override fun createOffer() {
        log.i { "create offer clicked " }
        rootNavigator.navigate(Routes.CreateOfferDirection.name)
    }

    override fun chatForOffer(offer: OfferListItemVO) {
        log.i { "chat for offer clicked " }
    }

    override fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }
}
