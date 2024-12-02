package network.bisq.mobile.domain.service.controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.utils.Logging
import platform.BackgroundTasks.*

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NotificationServiceController: ServiceController, Logging {

    private val logScope = CoroutineScope(Dispatchers.Main)

    actual override fun startService() {
        logDebug("Starting background service")
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier = "network.bisq.mobile.ios.backgroundtask", usingQueue = null) { task ->
            handleBackgroundTask(task as BGProcessingTask)
        }
        scheduleBackgroundTask()
        logDebug("Background service started")
    }

    actual override fun stopService() {
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
        logDebug("Background service stopped")
    }

    actual fun pushNotification(title: String, message: String) {
//        TODO
//        val content = UNMutableNotificationContent().apply {
//            this.title = title
//            this.body = message
//        }
//        val request = UNNotificationRequest.requestWithIdentifier(
//            NSUUID().UUIDString,
//            content,
//            null
//        )
//        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    actual override fun isServiceRunning(): Boolean {
        // iOS doesn't allow querying background task state directly
        return false
    }

    private fun handleBackgroundTask(task: BGProcessingTask) {
        logDebug("Executing background task")
        task.setTaskCompletedWithSuccess(true)
        scheduleBackgroundTask() // Reschedule if needed
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleBackgroundTask() {
        val request = BGProcessingTaskRequest("com.yourapp.backgroundtask").apply {
            requiresNetworkConnectivity = true
        }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        logDebug("Background task scheduled")
    }

    private fun logDebug(message: String) {
        logScope.launch {
            log.d { message }
        }
    }
}
