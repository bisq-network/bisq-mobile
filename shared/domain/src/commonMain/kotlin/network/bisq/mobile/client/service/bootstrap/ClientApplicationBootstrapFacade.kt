package network.bisq.mobile.client.service.bootstrap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.TrustedNodeService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.i18n.i18n

class ClientApplicationBootstrapFacade(
    private val settingsRepository: SettingsRepository,
    private val trustedNodeService: TrustedNodeService
) : ApplicationBootstrapFacade() {

    private val ioScope = CoroutineScope(IODispatcher)
    override fun activate() {
        if (isActive) {
            return
        }
        log.d { "Running bootstrap." }
        // TODO all texts here shoul use the translation module
        setState("Bootstrapping..")
        setProgress(0f)

        // just dummy loading simulation, might be that there is no loading delay at the end...
        ioScope.launch {
            settingsRepository.fetch()
            val url = settingsRepository.data.value?.bisqApiUrl
            log.d { "Settings url $url" }

//            TODO this is validated elsewhere, need to unify it or get
//            rid of this facade otherwise
//            Main issue is that the Trusted node setup screen is not
//            if (url == null) {
//                setState("Trusted node not configured")
//                setProgress(0f)
//            } else {
            if (trustedNodeService.isDemo()) {
                isDemo = true
            }
            setProgress(0.5f)
            setState("Connecting to Trusted Node..")
            if (!trustedNodeService.isConnected) {
                try {
                    if (!trustedNodeService.isDemo()) {
                        trustedNodeService.connect()
                    }
                    setState("bootstrap.connectedToTrustedNode".i18n())
                    setProgress(1.0f)
                } catch (e: Exception) {
                    log.e(e) { "Failed to connect to trusted node" }
                    setState("No connectivity")
                    setProgress(1.0f)
                }
            } else {
                setState("bootstrap.connectedToTrustedNode".i18n())
                setProgress(1.0f)
            }
//            }
        }

        isActive = true
        log.d { "Running bootstrap finished." }
    }

    override fun deactivate() {
        isActive = false
    }
}