package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter

class TabContainerPresenter(
    private val mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
) : BasePresenter(mainPresenter), ITabContainerPresenter {

    private val _tradesWithUnreadMessages: MutableStateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> = _tradesWithUnreadMessages

    private var job: Job? = null

    override fun onViewAttached() {
        super.onViewAttached()

        job = presenterScope.launch {
            mainPresenter.tradesWithUnreadMessages.collect{ _tradesWithUnreadMessages.value = it }
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