package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.domain.utils.Logging

/**
 * No-op implementation of PushNotificationServiceFacade for Android client app.
 * Android uses:
 *
 *  - Connect app: WebSocket-based notifications instead of native push notifications.
 *  - Node (Bisq Easy) app: Full node gets updates and push notifications to the device
 *
 * When FCM support is added in the future, this can be replaced with a proper
 * AndroidPushNotificationServiceFacade implementation.
 */
class NoOpClientPushNotificationServiceFacade :
    ServiceFacade(),
    PushNotificationServiceFacade,
    Logging {
    private val _isPushNotificationsEnabled = MutableStateFlow(false)
    override val isPushNotificationsEnabled: StateFlow<Boolean> = _isPushNotificationsEnabled.asStateFlow()

    private val _isDeviceRegistered = MutableStateFlow(false)
    override val isDeviceRegistered: StateFlow<Boolean> = _isDeviceRegistered.asStateFlow()

    private val _deviceToken = MutableStateFlow<String?>(null)
    override val deviceToken: StateFlow<String?> = _deviceToken.asStateFlow()

    override suspend fun activate() {
        super<ServiceFacade>.activate()
        log.i { "Android client app uses WebSocket-based notifications - native push notifications not active" }
    }

    override suspend fun requestPermission(): Boolean {
        log.d { "Native push notifications not supported on Android client app - using WebSocket notifications" }
        return false
    }

    override suspend fun registerForPushNotifications(): Result<Unit> {
        log.d { "Native push notifications not supported on Android client app - using WebSocket notifications" }
        return Result.success(Unit)
    }

    override suspend fun unregisterFromPushNotifications(): Result<Unit> {
        log.d { "Native push notifications not supported on Android client app - using WebSocket notifications" }
        return Result.success(Unit)
    }

    override suspend fun onDeviceTokenReceived(token: String) {
        // No-op
    }

    override suspend fun onDeviceTokenRegistrationFailed(error: Throwable) {
        // No-op
    }
}
