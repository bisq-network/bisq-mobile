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
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import kotlin.concurrent.Volatile

class ForegroundServiceControllerImpl(
    private val notificationController: NotificationController,
) : ForegroundServiceController,
    Logging {
    companion object {
        // BACKGROUND_TASK_ID is `${current bundle id}.background-processing`. The bundle
        // id varies per build configuration (see iosClient.xcodeproj):
        //   • Release + sdk=iphoneos (TestFlight / App Store): bisq.mobile.client.BisqConnect
        //   • Debug (sim or device):                          network.bisq.mobile.ios
        //
        // Every value this can resolve to MUST be listed in iosClient/iosClient/Info.plist
        // under BGTaskSchedulerPermittedIdentifiers, otherwise BGTaskScheduler.register(...)
        // raises NSInternalInconsistencyException and aborts the app at launch.
        // If a new build configuration adds another bundle id, add the corresponding
        // ".background-processing" entry to the plist.
        val BACKGROUND_TASK_ID: String by lazy {
            val base = NSBundle.mainBundle.bundleIdentifier ?: "network.bisq.mobile.ios"
            "$base.background-processing"
        }
        const val CHECK_NOTIFICATIONS_DELAY = 15 * 10000L

        @Volatile
        private var isBackgroundTaskHandlerRegistered: Boolean = false

        /**
         * Registers the BGTaskScheduler launch handler for [BACKGROUND_TASK_ID].
         *
         * **Must be called from `application(_:didFinishLaunchingWithOptions:)`** on the
         * main thread, synchronously, before that method returns. iOS asserts this
         * timing requirement internally and `abort()`s the app via
         * `NSInternalInconsistencyException` if violated — observed as the
         * `-[BGTaskScheduler _unsafe_registerForTaskWithIdentifier:usingQueue:launchHandler:]`
         * crash signature.
         *
         * Idempotent. Safe to call multiple times.
         *
         * The launch handler intentionally captures no instance state — Koin / DI may
         * not be initialised yet when this runs.
         */
        @OptIn(ExperimentalForeignApi::class)
        fun registerBackgroundTaskHandler() {
            if (isBackgroundTaskHandlerRegistered) return
            runCatching {
                BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
                    identifier = BACKGROUND_TASK_ID,
                    usingQueue = null,
                ) { task ->
                    val bgTask = task as BGProcessingTask
                    bgTask.setTaskCompletedWithSuccess(true)
                    scheduleBackgroundTaskStatic()
                }
                isBackgroundTaskHandlerRegistered = true
            }.onFailure {
                // The runCatching only catches Kotlin throwables; Apple raises an
                // Obj-C NSException that propagates past this and terminates the
                // process. Logged here for completeness in case Kotlin ever does throw.
            }
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun scheduleBackgroundTaskStatic() {
            val request =
                BGProcessingTaskRequest(BACKGROUND_TASK_ID).apply {
                    requiresNetworkConnectivity = true
                    // "now + 10s". timeIntervalSinceReferenceDate(10.0) would yield
                    // 2001-01-01 00:00:10 UTC — a past date that causes iOS to schedule
                    // the task immediately (defeating any debounce intent).
                    earliestBeginDate = NSDate(timeIntervalSinceReferenceDate = NSDate().timeIntervalSinceReferenceDate + 10.0)
                }
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val observerJobs = mutableMapOf<Flow<*>, Job>()

    private var isRunning = false
    private val isRunningMutex = Mutex()
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
                    // BGTaskScheduler.register(...) is performed at app launch from
                    // iosClient.swift's didFinishLaunchingWithOptions via the static
                    // [registerBackgroundTaskHandler]. Trying to register here (post-launch,
                    // off the main thread, after coroutine + mutex deferral) raises
                    // NSInternalInconsistencyException and aborts the app.
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

    override fun <T> registerObserver(
        flow: Flow<T>,
        onStateChange: suspend (T) -> Unit,
    ) {
        if (observerJobs.contains(flow)) {
            log.w { "State flow observer already registered, skipping registration" }
            return
        }
        val job =
            serviceScope.launch(Dispatchers.Default) {
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
        val request =
            BGProcessingTaskRequest(BACKGROUND_TASK_ID).apply {
                requiresNetworkConnectivity = true
                // "now + 10s" — see scheduleBackgroundTaskStatic for the rationale.
                earliestBeginDate = NSDate(timeIntervalSinceReferenceDate = NSDate().timeIntervalSinceReferenceDate + 10.0)
            }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        logDebug("Background task scheduled")
    }

    private fun logDebug(message: String) {
        logScope.launch {
            // (Dispatchers.Main)
            log.d { message }
        }
    }
}
