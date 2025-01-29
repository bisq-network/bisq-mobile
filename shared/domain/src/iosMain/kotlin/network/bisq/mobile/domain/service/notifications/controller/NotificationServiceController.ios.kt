package network.bisq.mobile.domain.service.notifications.controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging
import platform.BackgroundTasks.*
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.setValue
import platform.UserNotifications.*
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_sync


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NotificationServiceController(private val appForegroundController: AppForegroundController): ServiceController, Logging {

    companion object {
        const val BACKGROUND_TASK_ID = "network.bisq.mobile.iosUC4273Y485"
        const val CHECK_NOTIFICATIONS_DELAY = 15 * 10000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val observerJobs = mutableMapOf<StateFlow<*>, Job>()

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
            logDebug("Notification Service already started, skipping launch")
            return
        }
        stopService() // needed in iOS to clear the id registration and avoid duplicates
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
            } else {
                logDebug("Notification permission denied: ${error?.localizedDescription}")
            }
        }
    }

    actual override fun stopService() {
//        unregisterAllObservers()
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
        logDebug("Background service stopped")
        isRunning = false
    }


    actual override fun <T> registerObserver(stateFlow: StateFlow<T>, onStateChange: (T) -> Unit) {
        if (observerJobs.contains(stateFlow)) {
            log.w { "State flow observer already registered, skipping registration" }
            return
        }
        val job = serviceScope.launch {
            stateFlow.collect {
                onStateChange(it)
            }
        }
        observerJobs[stateFlow] = job
    }

    actual override fun unregisterObserver(stateFlow: StateFlow<*>) {
        observerJobs[stateFlow]?.cancel()
        observerJobs.remove(stateFlow)
    }

    private fun unregisterAllObservers() {
        observerJobs?.keys?.forEach { unregisterObserver(it) }
    }

    actual fun pushNotification(title: String, message: String) {
        if (isAppInForeground()) {
            log.w { "Skipping notification since app is in the foreground" }
            return
        }
        
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
        task.setTaskCompletedWithSuccess(true)
        logDebug("Background task completed successfully")
        scheduleBackgroundTask()
    }

    private fun startBackgroundTaskLoop() {
        isRunning = true
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

    private fun logDebug(message: String) {
        logScope.launch { // (Dispatchers.Main)
            log.d { message }
        }
    }


    // in iOS this needs to be done on app init or it will throw exception
    fun registerBackgroundTask() {
        if (isBackgroundTaskRegistered) {
            logDebug("Background task is already registered, skipping registration launch")
            return
        }

        runCatching {
            BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
                identifier = BACKGROUND_TASK_ID,
                usingQueue = null
            ) { task ->
                handleBackgroundTask(task as BGProcessingTask)
            }

            isBackgroundTaskRegistered = true
            logDebug("Background task handler registered")
        }.onFailure {
            log.e(it) { "Failed to register background task - notifications won't work" }
            isBackgroundTaskRegistered = false
        }
    }

    actual fun isAppInForeground(): Boolean {
        // for iOS we handle this externally
        return false
    }
}
