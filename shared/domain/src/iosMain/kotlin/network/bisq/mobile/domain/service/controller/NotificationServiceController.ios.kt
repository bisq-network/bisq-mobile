package network.bisq.mobile.domain.service.controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.utils.Logging
import platform.BackgroundTasks.*
import platform.Foundation.NSUUID
import platform.Foundation.setValue
import platform.UserNotifications.*
import platform.darwin.NSObject


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NotificationServiceController: ServiceController, Logging {

    companion object {
        const val BACKGROUND_TASK_ID = "network.bisq.mobile.ios.backgroundtask"
    }

    private var isRunning = false
    private var isBackgroundTaskRegistered = false
    private val logScope = CoroutineScope(Dispatchers.Main)

    init {
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
    }


    actual override fun startService() {
        if (isRunning) {
            return
        }
        logDebug("Starting background service")
//            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
//                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
//            ) { granted, error ->
//                if (granted) {
//                    logDebug("Notification permission granted.")
//
////                  TODO need to move to iOS callback ->  didFinishLaunchingWithOptions
//                    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier = BACKGROUND_TASK_ID, usingQueue = null) { task ->
//                        handleBackgroundTask(task as BGProcessingTask)
//                    }
//                    scheduleBackgroundTask()
//                    logDebug("Background service started")
//                    isRunning = true
//                } else {
//                    logDebug("Notification permission denied: ${error?.localizedDescription}")
//                }
//            }

        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (granted) {
                logDebug("Notification permission granted.")
                // Once permission is granted, you can start scheduling background tasks
                scheduleBackgroundTask()
                logDebug("Background service started")
                isRunning = true
            } else {
                logDebug("Notification permission denied: ${error?.localizedDescription}")
            }
        }
    }

    actual override fun stopService() {
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
        println("Background service stopped")
        isRunning = false
    }

    actual fun pushNotification(title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            setValue(title, forKey = "title")
            setValue(message, forKey = "body")
            setSound(UNNotificationSound.defaultSound())
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            NSUUID().UUIDString,  // Generates a unique identifier
            content,
            null  // Trigger can be set to null for immediate delivery
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error adding notification request: ${error.localizedDescription}")
            } else {
                println("Notification added successfully")
            }
        }
    }

    actual override fun isServiceRunning(): Boolean {
        // iOS doesn't allow querying background task state directly
        return isRunning
    }

    private fun handleBackgroundTask(task: BGProcessingTask) {
        logDebug("Executing background task")
        task.setTaskCompletedWithSuccess(true)
        scheduleBackgroundTask() // Reschedule if needed
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleBackgroundTask() {
        val request = BGProcessingTaskRequest(BACKGROUND_TASK_ID).apply {
            requiresNetworkConnectivity = true
        }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        logDebug("Background task scheduled")
    }


    private fun logDebug(message: String) {
        logScope.launch {
            log.d(message)
        }
    }


    private fun registerBackgroundTask() {
        if (isBackgroundTaskRegistered) {
            logDebug("Background task is already registered.")
            return
        }

        // Register for background task handler
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier = BACKGROUND_TASK_ID, usingQueue = null) { task ->
            handleBackgroundTask(task as BGProcessingTask)
        }

        isBackgroundTaskRegistered = true
        logDebug("Background task handler registered.")

    }
}
