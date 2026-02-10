package network.bisq.mobile.client.common.domain.service.bootstrap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.access.DEMO_API_URL
import network.bisq.mobile.client.common.domain.access.session.SessionResponse
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val webSocketClientService: WebSocketClientService,
    kmpTorService: KmpTorService,
    private val sessionService: SessionService,
) : ApplicationBootstrapFacade(kmpTorService) {
    override suspend fun activate() {
        super.activate()

        setState("mobile.clientApplicationBootstrap.bootstrapping".i18n())
        setProgress(0f)

        val settings = sensitiveSettingsRepository.fetch()
        if (settings.selectedProxyOption == BisqProxyOption.INTERNAL_TOR) {
            observeTorState()
        } else {
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
                            delay(100)
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
                            )

                        sensitiveSettingsRepository.update { updatedSettings }

                        serviceScope.launch {
                            // Without delay its not working
                            delay(100)
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
                setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else {
                log.e(error) { "Failed to connect to trusted node: ${error.message}" }
                setState("mobile.bootstrap.noConnectivity".i18n())
                setProgress(1.0f)
            }
        }
    }

    override fun onTorStarted() {
        onTorStartedOrSkipped()
    }

    override suspend fun deactivate() {
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
