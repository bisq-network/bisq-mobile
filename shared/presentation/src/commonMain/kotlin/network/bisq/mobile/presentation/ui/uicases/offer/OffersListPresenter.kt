package network.bisq.mobile.presentation.ui.uicases.offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes


open class OffersListPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), IOffersListPresenter {
    override val offerListItems: StateFlow<List<OfferListItem>> = offerbookServiceFacade.offerListItems

    private val _selectedDirection = MutableStateFlow(network.bisq.mobile.domain.replicated.offer.Direction.SELL)
    override val selectedDirection: StateFlow<network.bisq.mobile.domain.replicated.offer.Direction> = _selectedDirection

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }

    override fun takeOffer(offer: OfferListItem) {
        log.i { "take offer clicked " }
        //todo show take offer screen
        rootNavigator.navigate(Routes.TakeOfferTradeAmount.name)
    }

    override fun chatForOffer(offer: OfferListItem) {
        log.i { "chat for offer clicked " }
    }

    override fun onSelectDirection(direction: network.bisq.mobile.domain.replicated.offer.Direction) {
        _selectedDirection.value = direction
    }
}
