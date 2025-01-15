package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val webSocketClientProvider: WebSocketClientProvider
) : BasePresenter(mainPresenter), ITrustedNodeSetupPresenter {

    private val _bisqApiUrl = MutableStateFlow("")
    override val bisqApiUrl: StateFlow<String> = _bisqApiUrl

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    init {
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter"}
        CoroutineScope(BackgroundDispatcher).launch {
            try {
                settingsRepository.fetch()
                settingsRepository.data.value.let {
                    it?.let {
                        log.d { "Settings connected:${it.isConnected} url:${it.bisqApiUrl}" }
                        updateBisqApiUrl(it.bisqApiUrl)
                    }
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
    }

    override fun updateBisqApiUrl(newUrl: String) {
        _bisqApiUrl.value = newUrl
        _isConnected.value = false
    }

    override fun testConnection() {
        backgroundScope.launch {
            val updatedSettings = (settingsRepository.data.value ?: Settings()).apply {
                bisqApiUrl = _bisqApiUrl.value
                isConnected = _isConnected.value
            }

            settingsRepository.update(updatedSettings)

            webSocketClientProvider.get().connect()
            if (webSocketClientProvider.get().isConnected) {
                showSnackbar("Connected successfully")
                _isConnected.value = true
                // showSnackbar("Connected successfully and long text message with long list of english words")
            } else {
                showSnackbar("Couldn't connect to given url ${_bisqApiUrl.value}, please try again with another setup")
                _isConnected.value = false
            }
        }
    }

    override fun navigateToNextScreen() {
        navigateTo(Routes.CreateProfile)
    }

    override fun goBackToSetupScreen() {
        navigateBack()
    }
}
