package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider
) : BasePresenter(mainPresenter), ITrustedNodeSetupPresenter {

    private val _isBisqApiUrlValid = MutableStateFlow(true)
    override val isBisqApiUrlValid: StateFlow<Boolean> = _isBisqApiUrlValid

    private val _isBisqApiVersionValid = MutableStateFlow(true)
    override val isBisqApiVersionValid: StateFlow<Boolean> = _isBisqApiVersionValid

    private val _bisqApiUrl = MutableStateFlow("ws://10.0.2.2:8090")
    override val bisqApiUrl: StateFlow<String> = _bisqApiUrl

    private val _trustedNodeVersion = MutableStateFlow("")
    override val trustedNodeVersion: StateFlow<String> = _trustedNodeVersion

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        presenterScope.launch {
            try {
                val data = withContext(IODispatcher) {
                    settingsRepository.fetch()
                }
                data?.let {
                    updateBisqApiUrl(it.bisqApiUrl, true)
                    validateVersion()
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
    }

    override fun updateBisqApiUrl(newUrl: String, isValid: Boolean) {
        // log.w { "$newUrl: $isValid" }
        _isBisqApiUrlValid.value = isValid
        _bisqApiUrl.value = newUrl
        _isConnected.value = false
    }

    override fun validateWsUrl(url: String): String? {
        val wsUrlPattern =
            """^(ws|wss):\/\/(([a-zA-Z0-9.-]+\.[a-zA-Z]{2,}|localhost)|(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}))(:\d{1,5})$""".toRegex()
        if (url.isEmpty()) {
            return "URL cannot be empty" //TODO:i18n
        }
        if (!wsUrlPattern.matches(url)) {
            return "Invalid WebSocket URL. Must be ws:// or wss:// followed by a domain/IP and port" //TODO:i18n
        }
        return null
    }

    override fun testConnection(isWorkflow: Boolean) {
        _isLoading.value = true
        log.d { "Test: ${_bisqApiUrl.value} isWorkflow $isWorkflow" }
        val connectionSettings = WebSocketClientProvider.parseUri(_bisqApiUrl.value)
        presenterScope.launch {
            val success = withContext(IODispatcher) {
                return@withContext webSocketClientProvider.testClient(connectionSettings.first, connectionSettings.second)
            }

            if (success) {
                val isCompatibleVersion = withContext(IODispatcher) {
                    updateTrustedNodeSettings()
                    delay(250L)
                    webSocketClientProvider.get().await()
                    validateVersion()
                }
                if (isCompatibleVersion) {
                    log.d { "Connected successfully to ${_bisqApiUrl.value} is workflow: $isWorkflow" }
                    showSnackbar("Connected successfully to ${_bisqApiUrl.value}, settings updated")
                    if (!isWorkflow) {
                        _isLoading.value = false
                        navigateBack()
                    }
                    _isConnected.value = true
                } else {
                    webSocketClientProvider.get().disconnect(isTest = true)
                    log.d { "Invalid version cannot connect" }
                    showSnackbar("Trusted node incompatible version, cannot connect")
                    _isConnected.value = false
                }
                _isLoading.value = false
            } else {
                showSnackbar("Could not connect to given url ${_bisqApiUrl.value}, please try again with another setup")
                _isConnected.value = false
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateTrustedNodeSettings() {
        val currentSettings = settingsRepository.fetch()
        val updatedSettings = Settings().apply {
            bisqApiUrl = _bisqApiUrl.value
            firstLaunch = currentSettings?.firstLaunch ?: true
        }
        settingsRepository.update(updatedSettings)
    }

    override fun navigateToNextScreen() {
        navigateTo(Routes.CreateProfile)
    }

    override fun goBackToSetupScreen() {
        navigateBack()
    }

    override suspend fun validateVersion(): Boolean {
        _isBisqApiVersionValid.value = false
        _trustedNodeVersion.value = settingsServiceFacade.getTrustedNodeVersion()
        if (settingsServiceFacade.isApiCompatible()) {
            _isBisqApiVersionValid.value = true
            return true
        } else {
            _isBisqApiVersionValid.value = false
            return false
        }
    }
}
