package network.bisq.mobile.domain.service.controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import platform.BackgroundTasks.*
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.setValue
import platform.UserNotifications.*
import platform.darwin.NSObject


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NotificationServiceController: ServiceController, Logging {

    companion object {
        const val BACKGROUND_TASK_ID = "network.bisq.mobile.iosUC4273Y485"
        const val CHECK_NOTIFICATIONS_DELAY = 15 * 10000L
    }

    private var isRunning = false
    private var isBackgroundTaskRegistered = false
    private val logScope = CoroutineScope(Dispatchers.Main)

    private fun setupDelegate() {
        val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
            override fun userNotificationCenter(
                center: UNUserNotificationCenter,
                willPresentNotification: UNNotification,
                withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
            ) {
                // Display alert, sound, or badge when the app is in the foreground
                withCompletionHandler(
                    UNNotificationPresentationOptionAlert or UNNotificationPresentationOptionSound or UNNotificationPresentationOptionBadge
                )
            }

            // Handle user actions on the notification
            override fun userNotificationCenter(
                center: UNUserNotificationCenter,
                didReceiveNotificationResponse: UNNotificationResponse,
                withCompletionHandler: () -> Unit
            ) {
                // Handle the response when the user taps the notification
                withCompletionHandler()
            }
        }

        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
        logDebug("Notification center delegate applied")
    }


    actual override fun startService() {
        if (isRunning) {
            return
        }
        logDebug("Starting background service")
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (granted) {
                logDebug("Notification permission granted.")
                setupDelegate()
                registerBackgroundTask()
                // Once permission is granted, you can start scheduling background tasks
                startBackgroundTaskLoop()
                logDebug("Background service started")
                isRunning = true
            } else {
                logDebug("Notification permission denied: ${error?.localizedDescription}")
            }
        }
    }

    actual override fun stopService() {
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
        logDebug("Background service stopped")
        isRunning = false
    }

    actual fun pushNotification(title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            setValue(title, forKey = "title")
            setValue(message, forKey = "body")
            setSound(UNNotificationSound.defaultSound())
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(5.0, repeats = false)

        val requestId = NSUUID().UUIDString
        val request = UNNotificationRequest.requestWithIdentifier(
            requestId,
            content,
            trigger
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error != null) {
                logDebug("Error adding notification request: ${error.localizedDescription}")
            } else {
                logDebug("Notification $requestId added successfully")
            }
        }
    }

    actual override fun isServiceRunning(): Boolean {
        // iOS doesn't allow querying background task state directly
        return isRunning
    }

    private fun handleBackgroundTask(task: BGProcessingTask) {
        task.setTaskCompletedWithSuccess(true)  // Mark the task as completed
        logDebug("Background task completed successfully")
        scheduleBackgroundTask()    // Re-schedule the next task
    }

    private fun startBackgroundTaskLoop() {
        CoroutineScope(Dispatchers.Default).launch {
            while (isRunning) {
                scheduleBackgroundTask()
                delay(CHECK_NOTIFICATIONS_DELAY) // Check notifications every min
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleBackgroundTask() {
        val request = BGProcessingTaskRequest(BACKGROUND_TASK_ID).apply {
            requiresNetworkConnectivity = true
            earliestBeginDate = NSDate(timeIntervalSinceReferenceDate = 10.0)
        }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        logDebug("Background task scheduled")
    }

//    fun setupTaskHandlers() {
//        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(BACKGROUND_TASK_ID, BGProcessingTask.class, ::handleBackgroundTask)
//    }


    private fun logDebug(message: String) {
        logScope.launch {
            log.d { message }
        }
    }


    // in iOS this needs to be done on app init or it will throw exception
    fun registerBackgroundTask() {
        if (isBackgroundTaskRegistered) {
            logDebug("Background task is already registered.")
            return
        }

        // Register for background task handler
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = BACKGROUND_TASK_ID,
            usingQueue = null
        ) { task ->
            handleBackgroundTask(task as BGProcessingTask)
        }

        isBackgroundTaskRegistered = true
        logDebug("Background task handler registered.")
    }
}
