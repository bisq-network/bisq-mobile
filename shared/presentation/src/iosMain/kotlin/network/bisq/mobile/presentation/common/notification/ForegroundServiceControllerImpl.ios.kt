package network.bisq.mobile.presentation.common.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate

import platform.Foundation.NSBundle


class ForegroundServiceControllerImpl(private val notificationController: NotificationController) :
    ForegroundServiceController, Logging {

    companion object {
        val BACKGROUND_TASK_ID: String by lazy {
            val base = NSBundle.mainBundle.bundleIdentifier ?: "network.bisq.mobile.ios"
            "$base.background-processing"
        }
        const val CHECK_NOTIFICATIONS_DELAY = 15 * 10000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val observerJobs = mutableMapOf<Flow<*>, Job>()

    private var isRunning = false
    private val isRunningMutex = Mutex()
    private var isBackgroundTaskRegistered = false
    private val logScope = CoroutineScope(Dispatchers.Main)

    override fun startService() {
        serviceScope.launch {
            isRunningMutex.withLock {
                if (isRunning) {
                    logDebug("Notification Service already started, skipping launch")
                    return@launch
                }
                isRunning = true

                stopService() // needed in iOS to clear the id registration and avoid duplicates
                logDebug("Starting background service")
                if (notificationController.hasPermission()) {
                    logDebug("Notification permission granted.")
                    registerBackgroundTask()
                    // Once permission is granted, you can start scheduling background tasks
                    startBackgroundTaskLoop()
                    logDebug("Background service started")
                } else {
                    logDebug("Notification permission denied")
                }
            }
        }
    }

    override fun stopService() {
//        unregisterAllObservers()
        BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
        logDebug("Background service stopped")
        serviceScope.launch {
            isRunningMutex.withLock {
                isRunning = false
            }
        }
    }


    override fun <T> registerObserver(flow: Flow<T>, onStateChange: (T) -> Unit) {
        if (observerJobs.contains(flow)) {
            log.w { "State flow observer already registered, skipping registration" }
            return
        }
        val job = serviceScope.launch(Dispatchers.Default) {
            try {
                flow.collect { onStateChange(it) }
            } catch (e: Exception) {
                log.e(e) { "Error in flow observer, flow collection terminated" }
            }
        }
        observerJobs[flow] = job
    }

    override fun unregisterObserver(flow: Flow<*>) {
        observerJobs[flow]?.cancel()
        observerJobs.remove(flow)
    }

    override fun unregisterObservers() {
        observerJobs.forEach { it.value.cancel() }
        observerJobs.clear()
    }

    override fun isServiceRunning(): Boolean {
        // iOS doesn't allow querying background task state directly
        return isRunning
    }

    override fun dispose() {
        unregisterObservers()
        serviceScope.cancel()
    }

    private fun handleBackgroundTask(task: BGProcessingTask) {
        task.setTaskCompletedWithSuccess(true)
        logDebug("Background task completed successfully")
        scheduleBackgroundTask()
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
}
