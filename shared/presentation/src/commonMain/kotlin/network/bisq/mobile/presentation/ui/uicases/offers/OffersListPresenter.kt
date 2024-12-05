package network.bisq.mobile.presentation.ui.uicases.offers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes


open class OffersListPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), IOffersListPresenter {
    override val offerListItems: StateFlow<List<OfferListItem>> = offerbookServiceFacade.offerListItems

    private val _selectedDirection = MutableStateFlow(Direction.SELL)
    override val selectedDirection: StateFlow<Direction> = _selectedDirection

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

    override fun onSelectDirection(direction: Direction) {
        _selectedDirection.value = direction
    }
}
