package network.bisq.mobile.client.bootstrap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade

class ClientApplicationBootstrapFacade(private val settingsRepository: SettingsRepository) :
    ApplicationBootstrapFacade() {

    override fun activate() {
        setState("Bootstrapping..")
        setProgress(0f)

        // just dummy loading simulation, might be that there is no loading delay at the end...
        CoroutineScope(BackgroundDispatcher).launch {
            settingsRepository.fetch()
            val url = settingsRepository.data.value?.bisqApiUrl
            log.d { "Settings url $url" }
            if (url == null) {
                setState("Trusted node not configured")
                setProgress(0f)
            } else {
                setProgress(0.25f)
                setState("Connecting to Trusted Node..")

                delay(50L)
                setState("Dummy state 3")
                setProgress(0.5f)

                delay(50L)
                setState("Dummy state 4")
                setProgress(1f)
            }
        }
    }

    override fun deactivate() {
    }
}