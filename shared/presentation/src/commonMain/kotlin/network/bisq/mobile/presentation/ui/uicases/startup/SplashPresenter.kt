package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

// TODO We should make it abstract and only use dependency which are covered by both node and client.
// WebSocketClientProvider should be only used in a ClientSplashPresenter
open class SplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider?
) : BasePresenter(mainPresenter) {

    open val state: StateFlow<String> get() = applicationBootstrapFacade.state
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

    private fun navigateToNextScreen() {
        log.d { "Navigating to next screen" }
        launchUI {
            if (isClientAndHasNoConnectivity()) {
                return@launchUI
            }

            handleDemoModeForClient()

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

    protected open fun navigateToTrustedNodeSetup() {
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

    // Node overrides that with returning false
    // TODO we should make it abstract and use a ClientSplashPresenter to make the differences more explicit
    open suspend fun isClientAndHasNoConnectivity(): Boolean {
        val provider = webSocketClientProvider ?: return false
        if (!provider.get().isConnected()) {
            log.d { "No connectivity detected, navigating to trusted node setup" }
            navigateToTrustedNodeSetup()
            return true
        }
        return false
    }

    // TODO we should make it abstract and use a ClientSplashPresenter
    open suspend fun handleDemoModeForClient() {
        if (webSocketClientProvider?.get()?.isDemo() == true) {
            ApplicationBootstrapFacade.isDemo = true
        }
    }

    fun onTimeoutDialogContinue() {
        applicationBootstrapFacade.extendTimeout()
    }

    fun onRestart() {
        restartApp()
    }

    fun onTerminateApp() {
        terminateApp()
    }
}
