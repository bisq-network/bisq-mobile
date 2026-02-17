package network.bisq.mobile.client.splash

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetup
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus
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
    private val connectivityService: ConnectivityService,
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
        private const val CONNECTIVITY_WAIT_TIMEOUT_MS = 10_000L
        private const val CONNECTIVITY_SAFETY_NET_TIMEOUT_MS = 20_000L
    }

    private var hasNavigated = false

    override val state: StateFlow<String> get() = applicationBootstrapFacade.state

    override fun onViewAttached() {
        super.onViewAttached()

        if (!ApplicationBootstrapFacade.isDemo) {
            // Safety net: if connectivity is not established within timeout,
            // redirect to trusted node setup regardless of bootstrap progress.
            presenterScope.launch {
                delay(CONNECTIVITY_SAFETY_NET_TIMEOUT_MS)
                if (!hasNavigated) {
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

        // In demo mode, always proceed (no real connectivity needed)
        if (!ApplicationBootstrapFacade.isDemo) {
            // Wait for ConnectivityService to confirm real data exchange with the backend,
            // rather than relying on raw WebSocket connection state which can be stale
            // during credential handoff.
            val connected =
                withTimeoutOrNull(CONNECTIVITY_WAIT_TIMEOUT_MS) {
                    connectivityService.status.first {
                        it == ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED ||
                            it == ConnectivityStatus.REQUESTING_INVENTORY
                    }
                    true
                } ?: false

            if (!connected) {
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
