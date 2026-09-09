package network.bisq.mobile.client.common.presentation.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Client-specific Support presenter with push notification debugging features.
 *
 * TODO: Coverage exclusion rationale - This presenter extends BasePresenter which requires
 * Koin DI and MainPresenter initialization. Unit testing would require complex mocking of
 * the entire presenter hierarchy. Consider integration tests with Koin if coverage is needed.
 */
@ExcludeFromCoverage
class ClientSupportPresenter(
    mainPresenter: MainPresenter,
    private val pushNotificationServiceFacade: PushNotificationServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(ClientSupportUiState())
    val uiState: StateFlow<ClientSupportUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        // Observe push notification state
        presenterScope.launch {
            pushNotificationServiceFacade.deviceToken.collect { token ->
                _uiState.update { it.copy(deviceToken = token) }
            }
        }

        presenterScope.launch {
            pushNotificationServiceFacade.isDeviceRegistered.collect { registered ->
                _uiState.update { it.copy(isDeviceRegistered = registered) }
            }
        }
    }

    fun onAction(action: ClientSupportUiAction) {
        when (action) {
            ClientSupportUiAction.OnRequestDeviceToken -> onRequestDeviceToken()
            is ClientSupportUiAction.OnCopyToken -> {
                copyToClipboard(action.token)
                showSnackbar("Token copied to clipboard")
            }
        }
    }

    private fun onRequestDeviceToken() {
        presenterScope.launch {
            _uiState.update { it.copy(tokenRequestInProgress = true) }
            try {
                val result = pushNotificationServiceFacade.registerForPushNotifications()
                if (result.isSuccess) {
                    showSnackbar("Device token retrieved successfully")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    showSnackbar("Failed to get device token: $errorMessage", type = SnackbarType.ERROR)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                showSnackbar("Error: $errorMessage", type = SnackbarType.ERROR)
            } finally {
                _uiState.update { it.copy(tokenRequestInProgress = false) }
            }
        }
    }
}

// Platform-specific clipboard function
expect fun copyToClipboard(text: String)
