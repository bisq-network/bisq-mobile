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


    @Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
    actual class NotificationServiceController: ServiceController, Logging {

        companion object {
            const val BACKGROUND_TASK_ID = "network.bisq.mobile.ios.backgroundtask"
        }

        private var isRunning = false

        private val logScope = CoroutineScope(Dispatchers.Main)

//      TODO foreground notifications?
//        UNUserNotificationCenter.currentNotificationCenter().delegate = object : UNUserNotificationCenterDelegateProtocol {
//            override fun userNotificationCenter(
//                center: UNUserNotificationCenter,
//                willPresentNotification: UNNotification,
//                withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
//            ) {
//                withCompletionHandler(UNNotificationPresentationOptionsAlert or UNNotificationPresentationOptionsSound)
//            }
//        }

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

//                  TODO need to move to iOS callback ->  didFinishLaunchingWithOptions
                    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier = BACKGROUND_TASK_ID, usingQueue = null) { task ->
                        handleBackgroundTask(task as BGProcessingTask)
                    }
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
            logDebug("Background service stopped")
            isRunning = false
        }

        actual fun pushNotification(title: String, message: String) {
            val content = UNMutableNotificationContent().apply {
                setValue(title, forKey = "title")
                setValue(message, forKey = "body")
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
    }
