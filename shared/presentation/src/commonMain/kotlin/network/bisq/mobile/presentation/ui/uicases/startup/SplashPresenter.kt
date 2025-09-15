package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class SplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientService: WebSocketClientService?,
) : BasePresenter(mainPresenter) {

    val state: StateFlow<String> get() = applicationBootstrapFacade.state
    val progress: StateFlow<Float> get() = applicationBootstrapFacade.progress
    val isTimeoutDialogVisible: StateFlow<Boolean> get() = applicationBootstrapFacade.isTimeoutDialogVisible
    val isBootstrapFailed: StateFlow<Boolean> get() = applicationBootstrapFacade.isBootstrapFailed
    val currentBootstrapStage: StateFlow<String> get() = applicationBootstrapFacade.currentBootstrapStage
    val shouldShowProgressToast: StateFlow<Boolean> get() = applicationBootstrapFacade.shouldShowProgressToast

    private var hasNavigatedAway = false

    override fun onViewAttached() {
        super.onViewAttached()
        
        collectUI(state) { value ->
            log.d { "Splash State: $value" }
        }
        
        collectUI(progress) { value ->
            if (value >= 1.0f && !hasNavigatedAway) {
                hasNavigatedAway = true
                navigateToNextScreen()
            }
        }

        collectUI(shouldShowProgressToast) { shouldShow ->
            if (shouldShow) {
                showSnackbar("bootstrap.progress.continuing".i18n(), isError = false)
                applicationBootstrapFacade.setShouldShowProgressToast(false)
            }
        }
    }

    private fun navigateToNextScreen() {
        log.d { "Navigating to next screen" }
        launchUI {
            // Check connectivity first
            if (!hasConnectivity()) {
                log.d { "No connectivity detected, navigating to trusted node setup" }
                navigateToTrustedNodeSetup()
                return@launchUI
            }

            if (webSocketClientService?.isDemo() == true) {
                ApplicationBootstrapFacade.isDemo = true
            }

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
                        // Scenario 3: Handle others based on app type
                        doCustomNavigationLogic(deviceSettings, hasProfile)
                    }
                }
            }.onFailure {
                if (it is CancellationException) return@launchUI
                log.e(it) { "Failed to navigate out of splash" }
            }
        }
    }

    private fun navigateToTrustedNodeSetup() {
        navigateTo(Routes.TrustedNodeSetup) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(Routes.Onboarding) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(Routes.CreateProfile) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    private fun navigateToHome() {
        navigateTo(Routes.TabContainer) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    open suspend fun hasConnectivity(): Boolean {
        return webSocketClientService?.isConnected() ?: false
    }

    private fun navigateToAgreement() {
        log.d { "Navigating to agreement" }
        navigateTo(Routes.Agreement) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    open fun doCustomNavigationLogic(settings: Settings, hasProfile: Boolean): Boolean {
        when {
            settings.bisqApiUrl.isEmpty() -> navigateToTrustedNodeSetup()
            settings.bisqApiUrl.isNotEmpty() && !hasProfile -> navigateToCreateProfile()
            else -> navigateToHome()
        }
        return true
    }

    fun onTimeoutDialogStop() {
        log.i { "User requested to stop bootstrap from timeout dialog" }
        launchIO {
            applicationBootstrapFacade.stopBootstrapForRetry()
        }
    }

    fun onTimeoutDialogContinue() {
        log.i { "User chose to continue waiting - extending timeout" }
        applicationBootstrapFacade.extendTimeout()
    }

    open fun onRestart() {
        log.i { "User requested app restart from failed state" }
        restartApp()
    }

    protected open fun restartApp() {
        // Default implementation - platform-specific implementations will override
        log.w { "App restart not implemented for this platform" }
    }
}
