package network.bisq.mobile.client.common.domain.service.bootstrap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.access.DEMO_API_URL
import network.bisq.mobile.client.common.domain.access.session.SessionResponse
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val webSocketClientService: WebSocketClientService,
    private val httpClientService: HttpClientService,
    kmpTorService: KmpTorService,
    private val sessionService: SessionService,
    private val connectivityService: ConnectivityService,
) : ApplicationBootstrapFacade(kmpTorService) {
    private companion object {
        // How long the completed "Load data" step (✓ / "Ready") is held on screen before navigating,
        // so the second step is perceptible instead of flashing by on fast (clearnet) connections.
        const val LOADING_DATA_DONE_DWELL_MS = 700L
    }

    /**
     * Typed bootstrap phases for the Connect app, replacing the untyped [state] string as the
     * source of truth for the 2-phase splash design (Connecting → Loading data). Data-sync, which
     * previously only lived in the separate [ConnectivityService] state machine, is folded into the
     * [CONNECTED] transition by observing [ConnectivityService.status] during [LOADING_DATA].
     */
    enum class ConnectBootstrapPhase {
        STARTING_TOR,
        CONNECTING,
        LOADING_DATA,
        CONNECTED,
    }

    private val _bootstrapPhase = MutableStateFlow(ConnectBootstrapPhase.CONNECTING)
    val bootstrapPhase: StateFlow<ConnectBootstrapPhase> = _bootstrapPhase.asStateFlow()

    // True only when the phone bootstraps its own embedded Tor (INTERNAL_TOR) to reach the node.
    // Drives whether the splash strip shows the dedicated Tor phase node.
    private val _usesInternalTor = MutableStateFlow(false)
    val usesInternalTor: StateFlow<Boolean> = _usesInternalTor.asStateFlow()

    private var connectivityObserverJob: Job? = null

    override suspend fun activate() {
        super.activate()

        connectivityObserverJob?.cancel()
        connectivityObserverJob = null

        setState("mobile.clientApplicationBootstrap.bootstrapping".i18n())
        setProgress(0f)

        val settings = sensitiveSettingsRepository.fetch()
        if (settings.selectedProxyOption == BisqProxyOption.INTERNAL_TOR) {
            _usesInternalTor.value = true
            _bootstrapPhase.value = ConnectBootstrapPhase.STARTING_TOR
            observeTorState()
        } else {
            _usesInternalTor.value = false
            _bootstrapPhase.value = ConnectBootstrapPhase.CONNECTING
            onTorStartedOrSkipped()
        }
    }

    fun onTorStartedOrSkipped() {
        onInitialized()
        serviceScope.launch(Dispatchers.Default) {
            val currentSettings = sensitiveSettingsRepository.fetch()

            val clientAuthState: ClientAuthState =
                resolveClientAuthState(currentSettings)

            val url = currentSettings.bisqApiUrl
            log.d { "Settings url $url" }

            when (clientAuthState) {
                ClientAuthState.REQUIRE_PAIRING -> {
                    // fresh install scenario, let it proceed to onboarding
                    setState("mobile.bootstrap.preparingInitialSetup".i18n())
                    setProgress(1.0f)
                }

                ClientAuthState.RENEW_SESSION -> {
                    // Defensive check: if clientId or clientSecret is null, fall back to pairing flow
                    val clientId = currentSettings.clientId
                    val clientSecret = currentSettings.clientSecret
                    if (clientId == null || clientSecret == null) {
                        log.w { "RENEW_SESSION state but clientId or clientSecret is null, falling back to pairing flow" }
                        setState("mobile.bootstrap.preparingInitialSetup".i18n())
                        setProgress(1.0f)
                        return@launch
                    }

                    // Check for demo mode - skip session renewal and go directly to connect
                    if (currentSettings.bisqApiUrl == DEMO_API_URL) {
                        log.i { "Demo mode detected - skipping session renewal" }
                        isDemo = true
                        setProgress(0.5f)
                        setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())
                        serviceScope.launch {
                            httpClientService.awaitClientReady()
                            connect()
                        }
                        return@launch
                    }

                    setProgress(0.5f)
                    setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())

                    val result =
                        sessionService.requestSession(
                            clientId,
                            clientSecret,
                        )

                    if (result.isSuccess) {
                        log.i { "Requesting sessionId was successful" }
                        val response: SessionResponse = result.getOrThrow()
                        val updatedSettings =
                            currentSettings.copy(
                                sessionId = response.sessionId,
                                sessionExpiresAt = response.expiresAt,
                            )

                        sensitiveSettingsRepository.update { updatedSettings }

                        serviceScope.launch {
                            // Wait for HttpClientService to pick up new sessionId
                            httpClientService.awaitClientReady()
                            connect()
                        }
                    } else {
                        log.i { "Requesting sessionId failed" }
                        // Either trusted node is offline or pairing data are
                        // not valid anymore (node has another LAN address or
                        // pairing ID is expired)
                        // TODO show info in pairing screen to redo pairing
                        setState("mobile.bootstrap.preparingInitialSetup".i18n())
                        setProgress(1.0f)
                    }
                }
            }
        }
    }

    private fun connect() {
        serviceScope.launch {
            val error = webSocketClientService.connect()
            if (error == null) {
                if (isDemo) {
                    // Demo has no real inventory sync; complete immediately as before so
                    // ClientSplashPresenter (which skips the connectivity wait in demo) navigates.
                    _bootstrapPhase.value = ConnectBootstrapPhase.CONNECTED
                    setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                    setProgress(1.0f)
                } else {
                    // WebSocket is up; enter the "Loading data" phase and let inventory sync
                    // (observed via ConnectivityService) drive completion.
                    _bootstrapPhase.value = ConnectBootstrapPhase.LOADING_DATA
                    setState("mobile.bootstrap.connect.step.loadingData".i18n())
                    setProgress(0.5f)
                    observeConnectivityForDataLoad()
                }
            } else {
                log.e(error) { "Failed to connect to trusted node: ${error.message}" }
                setState("mobile.bootstrap.noConnectivity".i18n())
                setProgress(1.0f)
            }
        }
    }

    /**
     * While in [ConnectBootstrapPhase.LOADING_DATA], watch [ConnectivityService.status] and complete
     * bootstrap as soon as the connection is usable — i.e. [ConnectivityStatus.isConnected], which is
     * already true at [ConnectivityStatus.REQUESTING_INVENTORY]. This mirrors the pre-redesign timing:
     * the app proceeds to home while remaining inventory streams in the background.
     *
     * Do NOT gate completion on the strictly-later [ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED]:
     * that state requires ClientConnectivityService.isSlow() to become false, i.e. the process-global
     * round-trip average to drop below threshold — and that average is fed only by REST requests
     * (WebSocketApiClient), which predominantly happen on the home screen, not while sitting on the
     * splash. Gating on it deadlocks: the status loops on REQUESTING_INVENTORY until the splash
     * safety-net timeout redirects to the trusted-node-setup error screen.
     *
     * Progress reaching 1.0 triggers the base presenter's navigation; the
     * [ConnectivityStatus.CONNECTED_WITH_LIMITATIONS] case also satisfies isConnected() and completes,
     * so ClientSplashPresenter.navigateToNextScreen() runs its existing limitations handling.
     *
     * internal (not private) to expose the connectivity→phase mapping as a same-module unit-test seam,
     * mirroring the base class's protected observeTorState().
     */

    internal fun observeConnectivityForDataLoad() {
        connectivityObserverJob?.cancel()
        connectivityObserverJob =
            serviceScope.launch {
                // Await the first usable connection (isConnected() is already true at REQUESTING_INVENTORY);
                // BOOTSTRAPPING / DISCONNECTED / RECONNECTING simply keep us in LOADING_DATA.
                val status = connectivityService.status.first { it.isConnected() }
                // Only the fully-received state advances the strip to "done"; REQUESTING_INVENTORY and
                // CONNECTED_WITH_LIMITATIONS complete bootstrap but keep the LOADING_DATA phase.
                if (status == ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED) {
                    _bootstrapPhase.value = ConnectBootstrapPhase.CONNECTED
                    setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                }
                // Hold the completed step (✓ / "Ready") on screen for a beat so it doesn't flash by on
                // fast (clearnet) connections before navigation triggers on progress = 1.0.
                delay(LOADING_DATA_DONE_DWELL_MS)
                setProgress(1.0f)
            }
    }

    override fun onTorStarted() {
        _bootstrapPhase.value = ConnectBootstrapPhase.CONNECTING
        onTorStartedOrSkipped()
    }

    override suspend fun deactivate() {
        connectivityObserverJob?.cancel()
        connectivityObserverJob = null
        super.deactivate()
    }

    private fun resolveClientAuthState(settings: SensitiveSettings): ClientAuthState =
        if (settings.bisqApiUrl.isEmpty() ||
            settings.clientName == null ||
            settings.clientId == null ||
            settings.clientSecret == null
        ) {
            ClientAuthState.REQUIRE_PAIRING
        } else {
            ClientAuthState.RENEW_SESSION
        }
}
