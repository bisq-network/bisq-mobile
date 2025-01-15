package network.bisq.mobile.presentation.ui.uicases

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter

class TabContainerPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
) : BasePresenter(mainPresenter), ITabContainerPresenter {

    override fun createOffer() {
        createOfferPresenter.onStartCreateOffer()
        navigateTo(Routes.CreateOfferDirection)
    }

}