package network.bisq.mobile.client.service.bootstrap

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.TrustedNodeService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val trustedNodeService: TrustedNodeService
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
            settingsRepository.fetch()
            val url = settingsRepository.data.value?.bisqApiUrl
            log.d { "Settings url $url" }

            if (trustedNodeService.isDemo()) {
                isDemo = true
            }
            setProgress(0.5f)
            setState("mobile.clientApplicationBootstrap.connectingToTrustedNode".i18n())
            if (trustedNodeService.isConnected) {
                setState("bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else if (url == null) {
                // fresh install scenario, let it proceed to onboarding
                setState("bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            } else {
                try {
                    if (!trustedNodeService.isDemo()) {
                        trustedNodeService.connect()
                        trustedNodeService.await()
                    }
                    setState("bootstrap.connectedToTrustedNode".i18n())
                    setProgress(1.0f)
                } catch (e: Exception) {
                    log.e(e) { "Failed to connect to trusted node: ${e.message}" }
                    setState("No connectivity")
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