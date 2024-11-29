package network.bisq.mobile.domain.service.controller

import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import platform.BackgroundTasks.*

actual class NotificationServiceController: ServiceController {

    actual override fun startService() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier("com.yourapp.backgroundtask") { task ->
            handleBackgroundTask(task as BGProcessingTask)
        }
        scheduleBackgroundTask()
    }

    actual override fun stopService() {
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
    }

    actual override fun pushNotification(title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            this.title = title
            this.body = message
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            NSUUID().UUIDString,
            content,
            null
        )
        UNUserNotificationCenter.currentNotificationCenter.addNotificationRequest(request, null)
    }

    actual override fun isServiceRunning(): Boolean {
        // iOS doesn't allow querying background task state directly
        return false
    }

    private fun handleBackgroundTask(task: BGProcessingTask) {
        task.setTaskCompletedWithSuccess(true)
        scheduleBackgroundTask() // Reschedule if needed
    }

    private fun scheduleBackgroundTask() {
        val request = BGProcessingTaskRequest("com.yourapp.backgroundtask").apply {
            requiresNetworkConnectivity = true
        }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
    }
}
