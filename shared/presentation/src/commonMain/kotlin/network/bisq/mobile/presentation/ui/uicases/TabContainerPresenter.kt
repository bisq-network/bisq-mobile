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
        try {
            createOfferPresenter.onStartCreateOffer()
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer" }
            showSnackbar(
                if (isDemo()) "Create offer is disabled in demo mode" else "Cannot create offer at this time, please try again later"
            )
        }
    }

}