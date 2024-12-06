package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class SplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository
) : BasePresenter(mainPresenter) {

    val state: StateFlow<String> = applicationBootstrapFacade.state
    val progress: StateFlow<Float> = applicationBootstrapFacade.progress

    private var job: Job? = null

    override fun onViewAttached() {
        job = backgroundScope.launch {
            progress.collect { value ->
                when {
                    value == 1.0f -> navigateToNextScreen()
                }
            }
        }
    }

    override fun onViewUnattaching() {
        cancelJob()
    }

    private fun navigateToNextScreen() {
        CoroutineScope(Dispatchers.Main).launch {

            settingsRepository.fetch()
            val settings: Settings = settingsRepository.data.value ?: Settings()

            if (userProfileService.hasUserProfile()) {
                // rootNavigator.navigate(Routes.TrustedNodeSetup.name) {
                // [DONE] For androidNode, goto TabContainer
                rootNavigator.navigate(Routes.TabContainer.name) {
                    popUpTo(Routes.Splash.name) { inclusive = true }
                }

                // TODO: This is only for xClient.
                // How to handle between xClient and androidNode
                if (settings.bisqApiUrl.isEmpty()) {
                    // Test if the Bisq remote instance is up and responding
                    // If yes, goto TabContainer screen.
                    // If no, goto TrustedNodeSetupScreen
                } else {
                    // If no, goto TrustedNodeSetupScreen
                }

            } else {
                // If firstTimeApp launch, goto Onboarding[clientMode] (androidNode / xClient)
                // If not, goto CreateProfile
                if (settings.firstLaunch) {
                    rootNavigator.navigate(Routes.Onboarding.name) {
                        popUpTo(Routes.Splash.name) { inclusive = true }
                    }
                } else {
                    rootNavigator.navigate(Routes.CreateProfile.name) {
                        popUpTo(Routes.Splash.name) { inclusive = true }
                    }
                }
            }
        }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}
