package network.bisq.mobile.domain.service.controller

/**
 * And interface for a controller of a notification service
 */
expect class NotificationServiceController: ServiceController {
    fun pushNotification(title: String, message: String)
    override fun startService()
    override fun stopService()
    override fun isServiceRunning(): Boolean
}