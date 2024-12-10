package network.bisq.mobile.domain.service.controller

/**
 * And interface for a controller of a notification service
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class NotificationServiceController: ServiceController {
    fun pushNotification(title: String, message: String)
    override fun startService()
    override fun stopService()
    override fun isServiceRunning(): Boolean
}