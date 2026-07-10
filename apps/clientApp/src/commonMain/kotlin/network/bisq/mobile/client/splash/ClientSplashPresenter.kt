package network.bisq.mobile.client.splash

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsSerializer
import network.bisq.mobile.client.common.domain.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.common.domain.service.bootstrap.ClientApplicationBootstrapFacade.ConnectBootstrapPhase
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.presentation.navigation.ClientNavRoute
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.uiString
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.startup.splash.SplashUiState
import kotlin.concurrent.Volatile

class ClientSplashPresenter(
    mainPresenter: MainPresenter,
    userProfileService: UserProfileServiceFacade,
    private val applicationBootstrapFacade: ClientApplicationBootstrapFacade,
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

    private val _clientUiState = MutableStateFlow(ClientSplashUiState())
    val clientUiState: StateFlow<ClientSplashUiState> = _clientUiState.asStateFlow()

    override fun applyRoute(route: NavRoute.Splash) {
        continueWithLimitations = route.continueWithLimitations
    }

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            combine(
                uiState,
                applicationBootstrapFacade.bootstrapPhase,
                applicationBootstrapFacade.torBootstrapProgress,
                applicationBootstrapFacade.usesInternalTor,
            ) { splashUiState, phase, torProgress, usesTor ->
                buildClientUiState(splashUiState, phase, torProgress, usesTor)
            }.collect { clientUiState ->
                _clientUiState.value = clientUiState
            }
        }

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
                        hasNavigated = true
                        val phase = applicationBootstrapFacade.bootstrapPhase.value
                        val wsConnected =
                            phase == ConnectBootstrapPhase.LOADING_DATA ||
                                phase == ConnectBootstrapPhase.CONNECTED
                        if (!wsConnected) {
                            // Still starting Tor or connecting — WebSocket never connected, genuine failure.
                            log.d { "Connectivity safety net triggered (${safetyNetTimeoutMs}ms, tor=$isTorConnection), navigating to trusted node setup" }
                            navigateToTrustedNodeSetup(showConnectionFailed = true)
                        } else {
                            // WS already connected (LOADING_DATA/CONNECTED): the node is reachable, so
                            // enter the app and let data finish loading in the background rather than
                            // showing a connection failure just because confirmation was slow.
                            log.d { "Connectivity safety net triggered (${safetyNetTimeoutMs}ms) but WS is connected, proceeding to app" }
                            proceedToAppSkippingConnectivityCheck()
                        }
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

    private fun buildClientUiState(
        splashUiState: SplashUiState,
        phase: ConnectBootstrapPhase,
        torProgress: Int,
        usesTor: Boolean,
    ): ClientSplashUiState =
        ClientSplashUiState(
            splashUiState = splashUiState,
            title = titleFor(phase),
            subtitle = subtitleFor(phase),
            progress = progressFor(phase, torProgress),
            showTorPhase = usesTor,
            torActive = phase == ConnectBootstrapPhase.STARTING_TOR,
            torDone = usesTor && phase != ConnectBootstrapPhase.STARTING_TOR,
            torDetail = torDetailFor(torProgress),
            connectingActive = phase == ConnectBootstrapPhase.CONNECTING,
            connectingDone =
                phase == ConnectBootstrapPhase.LOADING_DATA ||
                    phase == ConnectBootstrapPhase.CONNECTED,
            loadingDataActive = phase == ConnectBootstrapPhase.LOADING_DATA,
            loadingDataDone = phase == ConnectBootstrapPhase.CONNECTED,
            connectingDetail = connectingDetailFor(phase),
            loadingDetail = UiString("mobile.bootstrap.connect.step.loadingData.detail"),
        )

    // Visual progress for the bottom bar, derived from the phase so a failure-driven
    // splashUiState.progress = 1.0 never fills the bar while the strip is still connecting.
    // During STARTING_TOR the live Tor % animates the bar continuously within the Tor band.
    private fun progressFor(
        phase: ConnectBootstrapPhase,
        torProgress: Int,
    ): Float =
        when (phase) {
            ConnectBootstrapPhase.STARTING_TOR -> (torProgress / 100f) * 0.35f
            ConnectBootstrapPhase.CONNECTING -> 0.45f
            ConnectBootstrapPhase.LOADING_DATA -> 0.7f
            ConnectBootstrapPhase.CONNECTED -> 1.0f
        }

    // Exhaustive when over the phase (no ordinal arithmetic) so a new phase is a compile error here.
    private fun titleFor(phase: ConnectBootstrapPhase): UiString =
        when (phase) {
            ConnectBootstrapPhase.STARTING_TOR -> UiString("mobile.bootstrap.connect.title.startingTor")
            ConnectBootstrapPhase.CONNECTING -> UiString("mobile.bootstrap.connect.title")
            ConnectBootstrapPhase.LOADING_DATA -> UiString("mobile.bootstrap.connect.title.loadingData")
            ConnectBootstrapPhase.CONNECTED -> UiString("mobile.bootstrap.connect.title.done")
        }

    private fun subtitleFor(phase: ConnectBootstrapPhase): UiString =
        when (phase) {
            ConnectBootstrapPhase.STARTING_TOR -> UiString("mobile.bootstrap.connect.subtitle.startingTor")
            ConnectBootstrapPhase.CONNECTING -> UiString("mobile.bootstrap.connect.subtitle")
            ConnectBootstrapPhase.LOADING_DATA -> UiString("mobile.bootstrap.connect.subtitle.loadingData")
            ConnectBootstrapPhase.CONNECTED -> UiString("mobile.bootstrap.connect.subtitle.done")
        }

    // Only the Connect phase shows a connecting detail; other phases have none (Tor has its own).
    private fun connectingDetailFor(phase: ConnectBootstrapPhase): UiString =
        when (phase) {
            ConnectBootstrapPhase.CONNECTING -> UiString("mobile.bootstrap.connect.step.connecting.detail")
            ConnectBootstrapPhase.STARTING_TOR,
            ConnectBootstrapPhase.LOADING_DATA,
            ConnectBootstrapPhase.CONNECTED,
            -> UiString("")
        }

    // Tor node detail, mirroring the node splash: "NN% — building circuit". Presenter emits the key +
    // the percentage arg; the composable resolves it.
    private fun torDetailFor(torProgress: Int): UiString = uiString("mobile.bootstrap.connect.step.tor.detail", torProgress)

    private suspend fun proceedToAppSkippingConnectivityCheck() {
        // WS is already connected; bypass navigateToNextScreen()'s connectivity re-check and route via
        // the base logic (home / onboarding / create-profile / agreement) as usual.
        super.navigateToNextScreen()
    }

    override suspend fun onNavigationDataUnavailable(error: Throwable) {
        // We got past the early no-config check, so the node IS configured; a failure here means we
        // couldn't reach it to load profile/settings. Route to the retry/pair screen rather than
        // sending an existing user through onboarding — regardless of any cached data.
        log.w(error) { "Could not load profile/settings; routing to trusted node setup" }
        navigateToTrustedNodeSetup(showConnectionFailed = true)
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
