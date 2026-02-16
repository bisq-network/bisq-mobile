package network.bisq.mobile.client.splash

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetup
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter

class ClientSplashPresenter(
    mainPresenter: MainPresenter,
    userProfileService: UserProfileServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientService: WebSocketClientService,
    versionProvider: VersionProvider,
) : SplashPresenter(
        mainPresenter,
        applicationBootstrapFacade,
        userProfileService,
        settingsRepository,
        settingsServiceFacade,
        versionProvider,
    ) {
    companion object {
        private const val CONNECTION_SETTLE_TIMEOUT_MS = 5_000L
        private const val CONNECTIVITY_SAFETY_NET_TIMEOUT_MS = 20_000L
    }

    private var hasNavigated = false

    override val state: StateFlow<String> get() = applicationBootstrapFacade.state

    override fun onViewAttached() {
        super.onViewAttached()

        if (!ApplicationBootstrapFacade.isDemo) {
            // Safety net: if WebSocket can't connect within timeout (e.g., stale TLS settings),
            // redirect to trusted node setup regardless of bootstrap progress.
            presenterScope.launch {
                delay(CONNECTIVITY_SAFETY_NET_TIMEOUT_MS)
                if (!hasNavigated && !webSocketClientService.isConnected()) {
                    log.d { "Connectivity safety net triggered, navigating to trusted node setup" }
                    hasNavigated = true
                    navigateToTrustedNodeSetup()
                }
            }
        }
    }

    override suspend fun navigateToNextScreen() {
        if (hasNavigated) return
        hasNavigated = true

        // In demo mode, always proceed (demo WebSocket is always "connected")
        if (!ApplicationBootstrapFacade.isDemo && !webSocketClientService.isConnected()) {
            // Connection may be temporarily disrupted during credential handoff
            // (e.g., session renewal triggers WebSocket client replacement).
            // Wait briefly for reconnection before redirecting to setup.
            val reconnected =
                withTimeoutOrNull(CONNECTION_SETTLE_TIMEOUT_MS) {
                    webSocketClientService.connectionState.first { it is ConnectionState.Connected }
                    true
                } ?: false

            if (!reconnected) {
                log.d { "No connectivity detected, navigating to trusted node setup" }
                navigateToTrustedNodeSetup()
                return
            }
        }
        super.navigateToNextScreen()
    }

    private fun navigateToTrustedNodeSetup() {
        navigateTo(TrustedNodeSetup) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }
}
