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
        backgroundScope.launch {
            try {
                settingsRepository.fetch()
                settingsRepository.data.value.let {
                    it?.let {
                        log.d { "Settings url:${it.bisqApiUrl}" }
                        updateBisqApiUrl(it.bisqApiUrl)
                    }
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
    }

    override fun updateBisqApiUrl(newUrl: String) {
        // TODO apply validation of the URL format ws://<IP>:<PORT> after Buddha's support for it
        _bisqApiUrl.value = newUrl
        _isConnected.value = false
    }

    override fun testConnection() {
        backgroundScope.launch {
            WebSocketClientProvider.parseUri(_bisqApiUrl.value).let { connectionSettings ->
                if (webSocketClientProvider.testClient(connectionSettings.first, connectionSettings.second)) {
                    updateTrustedNodeSettings()
                    _isConnected.value = true
                    showSnackbar("Connected successfully to ${_bisqApiUrl.value}, settings updated")
                    // showSnackbar("Connected successfully and long text message with long list of english words")
                } else {
                    showSnackbar("Could not connect to given url ${_bisqApiUrl.value}, please try again with another setup")
                    _isConnected.value = false
                }
            }
        }
    }

    private suspend fun updateTrustedNodeSettings() {
        val currentSettings = settingsRepository.fetch()
        val updatedSettings = Settings().apply {
            bisqApiUrl = _bisqApiUrl.value
            firstLaunch = currentSettings?.firstLaunch ?: false
        }
        settingsRepository.update(updatedSettings)
    }

    override fun navigateToNextScreen() {
        navigateTo(Routes.CreateProfile)
    }

    override fun goBackToSetupScreen() {
        navigateBack()
    }
}
