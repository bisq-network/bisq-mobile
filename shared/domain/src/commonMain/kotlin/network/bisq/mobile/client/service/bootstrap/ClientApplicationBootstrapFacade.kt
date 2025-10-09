package network.bisq.mobile.client.service.bootstrap

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val settingsRepository: SettingsRepository,
    private val webSocketClientProvider: WebSocketClientProvider,
) : ApplicationBootstrapFacade() {

    private var bootstrapJob: Job? = null

    override fun activate() {
        super.activate()

        setState("mobile.clientApplicationBootstrap.bootstrapping".i18n())
        setProgress(0f)

        bootstrapJob = serviceScope.launch {
            val url = settingsRepository.fetch().bisqApiUrl
            log.d { "Settings url $url" }

            if (url.isBlank()) {
                // fresh install scenario, let it proceed to onboarding
                setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else {
                setProgress(0.5f)
                setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())

                webSocketClientProvider.initialize()
                val error = webSocketClientProvider.connect()
                delay(200) // small delay to allow connection state to be collected // FIXME: refactor needed to remove this delay
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

        log.d { "Running bootstrap finished." }
    }

    override fun deactivate() {
        bootstrapJob?.cancel()
        bootstrapJob = null

        super.deactivate()
    }
}