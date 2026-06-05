package network.bisq.mobile.client.splash

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsSerializer
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.presentation.navigation.ClientNavRoute
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import kotlin.concurrent.Volatile

class ClientSplashPresenter(
    mainPresenter: MainPresenter,
    userProfileService: UserProfileServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    private val connectivityService: ConnectivityService,
    versionProvider: VersionProvider,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
) : SplashPresenter(
        mainPresenter,
        applicationBootstrapFacade,
        userProfileService,
        settingsRepository,
        settingsServiceFacade,
        versionProvider,
    ) {
    companion object {
        // Must exceed monitoring start delay + one full health cycle (period + health-check timeout).
        private const val CONNECTIVITY_WAIT_TIMEOUT_CLEARNET_MS =
            ClientConnectivityService.START_DELAY +
                ClientConnectivityService.PERIOD +
                ClientConnectivityService.TIMEOUT
        private const val CONNECTIVITY_WAIT_TIMEOUT_TOR_MS =
            ClientConnectivityService.START_DELAY_TOR +
                ClientConnectivityService.PERIOD +
                ClientConnectivityService.TIMEOUT

        // Safety net for clearnet connections: 45 s is generous for a TCP round trip.
        private const val CONNECTIVITY_SAFETY_NET_TIMEOUT_MS = 45_000L

        // Safety net for Tor (.onion) connections: Tor circuit establishment alone takes
        // 15–60 s; the subsequent WS upgrade and API-version probe add more.
        private const val CONNECTIVITY_SAFETY_NET_TIMEOUT_TOR_MS = 60_000L
    }

    private var hasNavigated = false

    @Volatile
    private var continueWithLimitations = false

    override val state: StateFlow<String> get() = applicationBootstrapFacade.state

    override fun applyRoute(route: NavRoute.Splash) {
        continueWithLimitations = route.continueWithLimitations
    }

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            // Check early if we have no saved trusted node configuration
            val settings = sensitiveSettingsRepository.fetch()
            if (settings.bisqApiUrl.isEmpty() || settings.clientId == null || settings.clientSecret == null) {
                val keystoreWasInvalidated = SensitiveSettingsSerializer.keystoreInvalidated.value
                if (keystoreWasInvalidated) {
                    log.w { "Keystore invalidated (OS upgrade?), credentials lost. User must re-pair." }
                } else {
                    log.d { "No saved trusted node configuration, navigating to pairing screen" }
                }
                hasNavigated = true
                navigateToTrustedNodeSetup(showKeystoreError = keystoreWasInvalidated)
                return@launch
            }

            // Only set up observers/safety net if we have valid settings
            if (!ApplicationBootstrapFacade.isDemo) {
                // React immediately when Tor or bootstrap fails (e.g., flight mode / no internet)
                presenterScope.launch {
                    applicationBootstrapFacade.torBootstrapFailed.first { it }
                    if (!hasNavigated) {
                        log.d { "Tor bootstrap failed, navigating to trusted node setup" }
                        hasNavigated = true
                        navigateToTrustedNodeSetup(showConnectionFailed = true)
                    }
                }

                presenterScope.launch {
                    applicationBootstrapFacade.isBootstrapFailed.first { it }
                    if (!hasNavigated) {
                        log.d { "Bootstrap failed, navigating to trusted node setup" }
                        hasNavigated = true
                        navigateToTrustedNodeSetup(showConnectionFailed = true)
                    }
                }

                // Safety net: if connectivity is not established within timeout, redirect to
                // trusted node setup. The budget is host-type-aware: Tor (.onion) circuits
                // take longer to establish than clearnet TCP.
                val isTorConnection = isTorConnection(settings.bisqApiUrl)
                val safetyNetTimeoutMs =
                    if (isTorConnection) {
                        CONNECTIVITY_SAFETY_NET_TIMEOUT_TOR_MS
                    } else {
                        CONNECTIVITY_SAFETY_NET_TIMEOUT_MS
                    }
                presenterScope.launch {
                    delay(safetyNetTimeoutMs)
                    if (!hasNavigated) {
                        log.d { "Connectivity safety net triggered (${safetyNetTimeoutMs}ms, tor=$isTorConnection), navigating to trusted node setup" }
                        hasNavigated = true
                        navigateToTrustedNodeSetup(showConnectionFailed = true)
                    }
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
            // during credential handoff. Timeout must exceed the monitoring start delay so
            // at least one health cycle can complete (especially on Tor).
            val settings = sensitiveSettingsRepository.fetch()
            val connectivityWaitTimeoutMs =
                if (isTorConnection(settings.bisqApiUrl)) {
                    CONNECTIVITY_WAIT_TIMEOUT_TOR_MS
                } else {
                    CONNECTIVITY_WAIT_TIMEOUT_CLEARNET_MS
                }
            val connected =
                withTimeoutOrNull(connectivityWaitTimeoutMs) {
                    connectivityService.status.first { it.isConnected() }
                    true
                } ?: false

            if (!connected) {
                log.d { "No connectivity detected, navigating to trusted node setup" }
                navigateToTrustedNodeSetup(showConnectionFailed = true)
                return
            }
            if (connectivityService.status.value == ConnectivityService.ConnectivityStatus.CONNECTED_WITH_LIMITATIONS) {
                if (continueWithLimitations) {
                    log.d { "Limited connectivity detected, continuing startup because route override is enabled" }
                    super.navigateToNextScreen()
                    return
                }
                log.d { "Limited connectivity detected, navigating to trusted node setup" }
                navigateToTrustedNodeSetup(showSubscriptionsFailed = true)
                return
            }
        }
        super.navigateToNextScreen()
    }

    private fun isTorConnection(bisqApiUrl: String): Boolean = bisqApiUrl.contains(".onion", ignoreCase = true)

    private fun navigateToTrustedNodeSetup(
        showConnectionFailed: Boolean = false,
        showKeystoreError: Boolean = false,
        showSubscriptionsFailed: Boolean = false,
    ) {
        navigateTo(
            ClientNavRoute.TrustedNodeSetup(
                showConnectionFailed = showConnectionFailed,
                showKeystoreError = showKeystoreError,
                showSubscriptionsFailed = showSubscriptionsFailed,
            ),
        ) {
            it.popUpTo<NavRoute.Splash> { inclusive = true }
        }
    }
}
