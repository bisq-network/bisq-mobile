package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class SplashPresenter(
    mainPresenter: MainPresenter,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
) : BasePresenter(mainPresenter) {

    val state: StateFlow<String> = applicationBootstrapFacade.state
    val progress: StateFlow<Float> = applicationBootstrapFacade.progress

    private var job: Job? = null

    override fun onViewAttached() {
        job = backgroundScope.launch {
            userRepository.fetch()?.let {
                it.lastActivity = Clock.System.now().toEpochMilliseconds()
                userRepository.update(it)
            }
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
            val settings: Settings = settingsRepository.fetch() ?: Settings()
            val user: User? = userRepository.fetch()

            if (user == null) {
                navigateToCreateProfile()
            } else {
                if (userProfileService.hasUserProfile()) {
                    if (!doCustomNavigationLogic(settings)) {
                        navigateToHome()
                    }
                } else {
                    // If firstTimeApp launch, goto Onboarding[clientMode] (androidNode / xClient)
                    // If not, goto CreateProfile
                    if (settings.firstLaunch) {
                        // TODO after onboarding need to make sure the rest is configured?
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
    }

    private fun navigateToCreateProfile() {
        rootNavigator.navigate(Routes.CreateProfile.name) {
            popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    private fun navigateToHome() {
        rootNavigator.navigate(Routes.TabContainer.name) {
            popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    /**
     * Default implementation in shared is for xClients. Override on node to avoid this.
     * @return true if handled, false otherwise
     */
    open fun doCustomNavigationLogic(settings: Settings): Boolean {
        when {
            settings.bisqApiUrl.isEmpty() -> {
                rootNavigator.navigate(Routes.TrustedNodeSetup.name) {
                    popUpTo(Routes.Splash.name) { inclusive = true }
                }
            }
//            settings.firstLaunch -> navigateToCreateProfile()
            else -> navigateToHome()
        }
        return true
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}
