package network.bisq.mobile.presentation.ui.uicases.trade.take_offer


import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class ReviewTradePresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), ITakeOfferReviewTradePresenter {

    override val offerListItems: StateFlow<List<OfferListItem>> = offerbookServiceFacade.offerListItems

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }

    override fun tradeConfirmed() {
        log.i { "Trade confirmed" }
        // TODO: Confirmation popup goes here
        rootNavigator.navigate(Routes.TradeFlow.name)
    }

}