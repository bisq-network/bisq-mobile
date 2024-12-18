package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class PaymentMethodPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), ITakeOfferPaymentMethodPresenter {

    override val offerListItems: StateFlow<List<OfferListItem>> = offerbookServiceFacade.offerListItems

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }

    override fun paymentMethodConfirmed() {
        log.i { "Payment method selected" }
        rootNavigator.navigate(Routes.TakeOfferReviewTrade.name)
    }

}
