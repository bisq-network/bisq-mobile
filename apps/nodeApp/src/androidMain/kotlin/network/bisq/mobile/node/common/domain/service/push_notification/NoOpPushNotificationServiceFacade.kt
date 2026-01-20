package network.bisq.mobile.node.common.domain.service.push_notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.push_notification.PushNotificationServiceFacade

/**
 * No-op implementation of PushNotificationServiceFacade for the node app.
 * The node app doesn't need push notifications since it's always running.
 */
class NoOpPushNotificationServiceFacade :
    ServiceFacade(),
    PushNotificationServiceFacade {
    private val _isPushNotificationsEnabled = MutableStateFlow(false)
    override val isPushNotificationsEnabled: StateFlow<Boolean> = _isPushNotificationsEnabled.asStateFlow()

    private val _isDeviceRegistered = MutableStateFlow(false)
    override val isDeviceRegistered: StateFlow<Boolean> = _isDeviceRegistered.asStateFlow()

    private val _deviceToken = MutableStateFlow<String?>(null)
    override val deviceToken: StateFlow<String?> = _deviceToken.asStateFlow()

    override suspend fun requestPermission(): Boolean = false

    override suspend fun registerForPushNotifications(): Result<Unit> = Result.success(Unit)

    override suspend fun unregisterFromPushNotifications(): Result<Unit> = Result.success(Unit)

    override suspend fun onDeviceTokenReceived(token: String) {
        // No-op
    }

    override suspend fun onDeviceTokenRegistrationFailed(error: Throwable) {
        // No-op
    }
}
