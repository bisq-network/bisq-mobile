package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute

abstract class SplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter) {

    abstract val state: StateFlow<String>

    val progress: StateFlow<Float> get() = applicationBootstrapFacade.progress
    val isTimeoutDialogVisible: StateFlow<Boolean> get() = applicationBootstrapFacade.isTimeoutDialogVisible
    val isBootstrapFailed: StateFlow<Boolean> get() = applicationBootstrapFacade.isBootstrapFailed
    val currentBootstrapStage: StateFlow<String> get() = applicationBootstrapFacade.currentBootstrapStage
    val shouldShowProgressToast: StateFlow<Boolean> get() = applicationBootstrapFacade.shouldShowProgressToast

    override fun onViewAttached() {
        super.onViewAttached()

        collectUI(state) { value ->
            log.d { "Splash State: $value" }
        }

        collectUI(progress) { value ->
            if (value >= 1.0f) {
                navigateToNextScreen()
            }
        }

        collectUI(shouldShowProgressToast) { shouldShow ->
            if (shouldShow) {
                showSnackbar("mobile.bootstrap.progress.continuing".i18n(), isError = false)
                applicationBootstrapFacade.setShouldShowProgressToast(false)
            }
        }
    }

    protected open suspend fun navigateToNextScreen() {
        log.d { "Navigating to next screen" }

        runCatching {
            val profileSettings: SettingsVO = settingsServiceFacade.getSettings().getOrThrow()
            val deviceSettings: Settings = settingsRepository.fetch()

            if (!profileSettings.isTacAccepted) {
                navigateToAgreement()
            } else {
                // only fetch profile with connectivity
                val hasProfile: Boolean = userProfileService.hasUserProfile()

                if (hasProfile) {
                    // Scenario 1: All good and setup for both androidNode and xClients
                    navigateToHome()
                } else if (deviceSettings.firstLaunch) {
                    // Scenario 2: Loading up for first time for both androidNode and xClients
                    navigateToOnboarding()
                } else {
                    // Scenario 3: Create profile
                    navigateToCreateProfile()
                }
            }
        }.onFailure {
            if (it is CancellationException) return
            log.e(it) { "Failed to navigate out of splash" }
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(NavRoute.Onboarding) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(NavRoute.CreateProfile) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    protected fun navigateToHome() {
        navigateTo(NavRoute.TabContainer) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    private fun navigateToAgreement() {
        log.d { "Navigating to agreement" }
        navigateTo(NavRoute.UserAgreement) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    fun onTimeoutDialogContinue() {
        applicationBootstrapFacade.extendTimeout()
    }

    fun onRestartApp() {
        restartApp()
    }

    fun onTerminateApp() {
        terminateApp()
    }
}
