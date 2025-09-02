package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter

/**
 * Main presenter for the display when landing the user on the app ready to be used.
 */
class TabContainerPresenter(
    private val mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter), ITabContainerPresenter {

    override val showAnimation: StateFlow<Boolean> get() = settingsServiceFacade.useAnimations
    override val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = mainPresenter.tradesWithUnreadMessages
    private var forceServicesReactivation = false

    override fun onViewAttached() {
        super.onViewAttached()

        onceOffReactivateServices()
    }

    override fun createOffer() {
        if (!isInteractive.value) return // This isInteractive UI blocker doesn't apply to FAB buttons
        disableInteractive()
        try {
            createOfferPresenter.onStartCreateOffer()
            createOfferPresenter.skipCurrency = false
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer: ${e.message}" }
        } finally {
            enableInteractive()
        }
    }

    private fun onceOffReactivateServices() {
        if (!forceServicesReactivation) {
            log.d { "User landed on home, reactivating services.." }
            launchIO {
                mainPresenter.reactivateServices()
                forceServicesReactivation = true
            }
        }
    }
}