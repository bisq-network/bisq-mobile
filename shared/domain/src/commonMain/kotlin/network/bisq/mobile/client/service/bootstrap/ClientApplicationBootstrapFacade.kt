package network.bisq.mobile.client.service.bootstrap

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.TrustedNodeService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val settingsRepository: SettingsRepository,
    private val trustedNodeService: TrustedNodeService
) : ApplicationBootstrapFacade() {

    private var bootstrapJob: Job? = null

    override fun activate() {
        super.activate()

        setState("mobile.clientApplicationBootstrap.bootstrapping".i18n())
        setProgress(0f)

        bootstrapJob = serviceScope.launch {
            val url = settingsRepository.fetch().bisqApiUrl
            log.d { "Settings url $url" }

            setProgress(0.5f)
            setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())
            if (trustedNodeService.isConnected()) {
                setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else {
                val error = trustedNodeService.connect()
                if (error == null) {
                    setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                    setProgress(1.0f)
                } else {
                    if (url.isBlank()) {
                        // fresh install scenario, let it proceed to onboarding
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

        log.d { "Running bootstrap finished." }
    }

    override fun deactivate() {
        bootstrapJob?.cancel()
        bootstrapJob = null

        super.deactivate()
    }
}