package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferPresenter

class TabContainerPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
) : BasePresenter(mainPresenter), ITabContainerPresenter {

    override fun createOffer() {
        createOfferPresenter.onStartCreateOffer()
        rootNavigator.navigate(Routes.CreateOfferDirection.name)
    }

}