package network.bisq.mobile.client.common.domain.service.bootstrap

import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.settings.domain.SensitiveSettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val webSocketClientService: WebSocketClientService,
    kmpTorService: KmpTorService,
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
        serviceScope.launch {
            val url = sensitiveSettingsRepository.fetch().bisqApiUrl
            log.d { "Settings url $url" }

            if (url.isBlank()) {
                // fresh install scenario, let it proceed to onboarding
                setState("mobile.bootstrap.preparingInitialSetup".i18n())
                setProgress(1.0f)
            } else {
                setProgress(0.5f)
                setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())

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
    }

    override fun onTorStarted() {
        onTorStartedOrSkipped()
    }

    override suspend fun deactivate() {
        super.deactivate()
    }
}
