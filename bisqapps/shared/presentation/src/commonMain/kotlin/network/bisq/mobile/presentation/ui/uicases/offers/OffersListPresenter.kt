package network.bisq.mobile.presentation.ui.uicases.offers

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class OffersListPresenter(
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IOffersList {

    override fun takeOffer() {
        rootNavigator.navigate(Routes.OfferList.name)
    }
}
