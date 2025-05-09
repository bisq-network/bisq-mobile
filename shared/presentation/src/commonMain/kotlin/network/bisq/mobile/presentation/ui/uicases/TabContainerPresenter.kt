package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter

class TabContainerPresenter(
    private val mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
) : BasePresenter(mainPresenter), ITabContainerPresenter {

    private val _unreadTrades: MutableStateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())
    override val unreadTrades: StateFlow<Map<String, Int>> = _unreadTrades

    private var job: Job? = null

    override fun onViewAttached() {
        super.onViewAttached()

        job = presenterScope.launch {
            mainPresenter.unreadTrades.collect{ _unreadTrades.value = it }
        }
    }

    override fun onViewUnattaching() {
        job?.cancel()
        job = null
        super.onViewUnattaching()
    }

    override fun createOffer() {
        try {
//            if (isDemo()) {
//                showSnackbar("Create offer is disabled in demo mode")
//                return
//            }
            createOfferPresenter.onStartCreateOffer()
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer" }
        }
    }

}