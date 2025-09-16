package network.bisq.mobile.client.presentation

import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter

class ClientSplashPresenter(
    mainPresenter: MainPresenter,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    userProfileService: UserProfileServiceFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientService: WebSocketClientService,
) : SplashPresenter(
    mainPresenter,
    applicationBootstrapFacade,
    userProfileService,
    settingsRepository,
    settingsServiceFacade,
) {

    override fun doCustomNavigationLogic(settings: Settings, hasProfile: Boolean): Boolean {
        when {
            settings.bisqApiUrl.isEmpty() -> navigateToTrustedNodeSetup()
            settings.bisqApiUrl.isNotEmpty() && !hasProfile -> navigateToCreateProfile()
            else -> navigateToHome()
        }
        return true
    }

    override suspend fun hasConnectivity(): Boolean {
        return webSocketClientService.isConnected()
    }

    override suspend fun navigateToNextScreen() {
        // Check connectivity first
        if (!hasConnectivity()) {
            log.d { "No connectivity detected, navigating to trusted node setup" }
            navigateToTrustedNodeSetup()
            return
        }

        if (webSocketClientService.isDemo()) {
            ApplicationBootstrapFacade.isDemo = true
        }
        super.navigateToNextScreen()
    }

    private fun navigateToTrustedNodeSetup() {
        navigateTo(Routes.TrustedNodeSetup) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    override fun restartApp() {
        // Default implementation - platform-specific implementations will override
        log.w { "App restart not implemented for this platform" }
    }
}
