package network.bisq.mobile.client.common.presentation.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Client-specific Support presenter with push notification debugging features.
 */
class ClientSupportPresenter(
    mainPresenter: MainPresenter,
    private val pushNotificationServiceFacade: PushNotificationServiceFacade,
) : BasePresenter(mainPresenter) {

    private val _deviceToken = MutableStateFlow<String?>(null)
    val deviceToken: StateFlow<String?> = _deviceToken.asStateFlow()
    
    private val _isDeviceRegistered = MutableStateFlow(false)
    val isDeviceRegistered: StateFlow<Boolean> = _isDeviceRegistered.asStateFlow()
    
    private val _tokenRequestInProgress = MutableStateFlow(false)
    val tokenRequestInProgress: StateFlow<Boolean> = _tokenRequestInProgress.asStateFlow()
    
    override fun onViewAttached() {
        super.onViewAttached()
        
        // Observe push notification state
        presenterScope.launch {
            pushNotificationServiceFacade.deviceToken.collect { token ->
                _deviceToken.value = token
            }
        }
        
        presenterScope.launch {
            pushNotificationServiceFacade.isDeviceRegistered.collect { registered ->
                _isDeviceRegistered.value = registered
            }
        }
    }
    
    fun onRequestDeviceToken() {
        presenterScope.launch {
            _tokenRequestInProgress.value = true
            try {
                val result = pushNotificationServiceFacade.registerForPushNotifications()
                if (result.isSuccess) {
                    showSnackbar("Device token retrieved successfully", isError = false)
                } else {
                    showSnackbar("Failed to get device token: ${result.exceptionOrNull()?.message}", isError = true)
                }
            } catch (e: Exception) {
                showSnackbar("Error: ${e.message}", isError = true)
            } finally {
                _tokenRequestInProgress.value = false
            }
        }
    }
    
    fun onCopyToken(token: String) {
        copyToClipboard(token)
        showSnackbar("Token copied to clipboard", isError = false)
    }
}

// Platform-specific clipboard function
expect fun copyToClipboard(text: String)

