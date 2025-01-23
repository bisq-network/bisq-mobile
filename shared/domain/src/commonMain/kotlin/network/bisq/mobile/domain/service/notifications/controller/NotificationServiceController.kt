package network.bisq.mobile.domain.service.notifications.controller

import kotlinx.coroutines.flow.StateFlow

/**
 * And interface for a controller of a notification service
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class NotificationServiceController : ServiceController {
    fun pushNotification(title: String, message: String)
    override fun startService()
    override fun stopService()
    override fun <T> registerObserver(stateFlow: StateFlow<T>, onStateChange: (T) -> Unit)
    override fun unregisterObserver(stateFlow: StateFlow<*>)
    override fun isServiceRunning(): Boolean
    fun isAppInForeground(): Boolean
}