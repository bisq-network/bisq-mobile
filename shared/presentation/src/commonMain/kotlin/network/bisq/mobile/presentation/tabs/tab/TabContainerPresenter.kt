package network.bisq.mobile.presentation.tabs.tab

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferPresenter

/**
 * Main presenter for the display when landing the user on the app ready to be used.
 */
class TabContainerPresenter(
    private val mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter),
    ITabContainerPresenter {
    override val showAnimation: StateFlow<Boolean> get() = settingsServiceFacade.useAnimations
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = mainPresenter.tradesWithUnreadMessages

    override fun onViewAttached() {
        super.onViewAttached()
    }

    override fun createOffer() {
        if (!isInteractive.value) return // This isInteractive UI blocker doesn't apply to FAB buttons
        disableInteractive()
        try {
            createOfferPresenter.onStartCreateOffer()
            createOfferPresenter.skipCurrency = false
            navigateTo(NavRoute.CreateOfferDirection)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer: ${e.message}" }
        } finally {
            enableInteractive()
        }
    }
}
