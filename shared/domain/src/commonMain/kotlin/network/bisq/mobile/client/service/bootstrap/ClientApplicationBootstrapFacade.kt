package network.bisq.mobile.client.service.bootstrap

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientService: WebSocketClientService,
) : ApplicationBootstrapFacade() {

    private var bootstrapJob: Job? = null

    override fun activate() {
        super.activate()

        if (isActive) {
            return
        }

        makeSureI18NIsReady(settingsServiceFacade.languageCode.value)

        setState("mobile.clientApplicationBootstrap.bootstrapping".i18n())
        setProgress(0f)

        bootstrapJob = serviceScope.launch {
            val url = settingsRepository.fetch().bisqApiUrl
            log.d { "Settings url $url" }

            if (webSocketClientService.isDemo()) {
                isDemo = true
            }
            setProgress(0.5f)
            setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())
            if (webSocketClientService.isConnected()) {
                setState("bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else if (url.isBlank()) {
                // fresh install scenario, let it proceed to onboarding
                setState("bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else {
                // web socket service automatically tries to connect
                val state = withTimeoutOrNull(15_000L) {
                    webSocketClientService.connectionState.filterNot { it is ConnectionState.Connecting }.first()
                } ?: ConnectionState.Disconnected(RuntimeException("Connection timed out after 15 seconds"))
                if (state is ConnectionState.Disconnected) {
                    if (state.error != null) {
                        log.e(state.error) { "Failed to connect to trusted node: ${state.error.message}" }
                    } else {
                        log.e(state.error) { "Websocket client was disconnected without error at bootstrap" }
                    }
                    setState("No connectivity")
                    setProgress(1.0f)
                } else if (state is ConnectionState.Connected) {
                    setState("bootstrap.connectedToTrustedNode".i18n())
                    setProgress(1.0f)
                }
            }
        }

        isActive = true
        log.d { "Running bootstrap finished." }
    }

    override suspend fun waitForTor() {
        // Client doesn't use Tor, so this returns immediately
        log.d { "Client bootstrap: waitForTor() - no Tor required atm, returning immediately" }
    }

    override fun deactivate() {
        bootstrapJob?.cancel()
        bootstrapJob = null
        isActive = false

        super.deactivate()
    }
}