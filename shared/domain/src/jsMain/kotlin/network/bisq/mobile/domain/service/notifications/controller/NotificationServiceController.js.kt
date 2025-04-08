package network.bisq.mobile.domain.service.notifications.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging

//    package network.bisq.mobile.domain.service.notifications.controller
//
//    import kotlinx.coroutines.CoroutineScope
//    import kotlinx.coroutines.SupervisorJob
//    import kotlinx.coroutines.flow.StateFlow
//    import kotlinx.coroutines.Job
//    import kotlinx.coroutines.launch
//    import network.bisq.mobile.domain.service.AppForegroundController
//    import network.bisq.mobile.domain.utils.Logging
//
//    /**
//     * JS-specific implementation of NotificationServiceController
//     */
//    actual class NotificationServiceController(private val appForegroundController: AppForegroundController) : ServiceController, Logging {
//
//        private val serviceScope = CoroutineScope(SupervisorJob())
//        private val observerJobs = mutableMapOf<StateFlow<*>, Job>()
//        private var isRunning = false
//
//        actual override fun startService() {
//            if (isRunning) {
//                log.w { "Service already running, skipping start call" }
//                return
//            }
//
//            // Check if browser supports notifications
//            if (js("'Notification' in window") as Boolean) {
//                // Request permission for notifications
//                js("""
//                Notification.requestPermission().then(function(permission) {
//                    if (permission === 'granted') {
//                        console.log('Notification permission granted.');
//                    } else {
//                        console.log('Notification permission denied.');
//                    }
//                });
//            """)
//                isRunning = true
//                log.i { "Started notification service" }
//            } else {
//                log.w { "This browser does not support notifications" }
//            }
//        }
//
//        actual override fun stopService() {
//            if (!isRunning) {
//                log.w { "Service is not running, skipping stop call" }
//                return
//            }
//
//            observerJobs.values.forEach { it.cancel() }
//            observerJobs.clear()
//            isRunning = false
//            log.i { "Stopped notification service" }
//        }
//
//        actual override fun <T> registerObserver(stateFlow: StateFlow<T>, onStateChange: (T) -> Unit) {
//            if (observerJobs.contains(stateFlow)) {
//                log.w { "State flow observer already registered, skipping registration" }
//                return
//            }
//
//            val job = serviceScope.launch {
//                stateFlow.collect {
//                    onStateChange(it)
//                }
//            }
//            observerJobs[stateFlow] = job
//        }
//
//        actual override fun unregisterObserver(stateFlow: StateFlow<*>) {
//            observerJobs[stateFlow]?.cancel()
//            observerJobs.remove(stateFlow)
//        }
//
//        actual override fun isServiceRunning(): Boolean {
//            return isRunning
//        }
//
//        actual fun pushNotification(title: String, message: String) {
//            if (isAppInForeground()) {
//                log.w { "Skipping notification since app is in the foreground" }
//                return
//            }
//
//            if (js("'Notification' in window && Notification.permission === 'granted'") as Boolean) {
//                js("""
//                new Notification(arguments[0], {
//                    body: arguments[1],
//                    icon: '/favicon.ico'
//                });
//            """)(title, message)
//                log.d { "Pushed notification: $title - $message" }
//            } else {
//                log.w { "Cannot push notification: permission not granted" }
//            }
//        }
//
//        actual fun isAppInForeground(): Boolean {
//            return appForegroundController.isForeground.value
//        }
//    }

actual class NotificationServiceController(
    private val appForegroundController: AppForegroundController
) : ServiceController, Logging {

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val observerJobs = mutableMapOf<StateFlow<*>, Job>()
    private var isRunning = false

    actual override fun startService() {
        if (isRunning) {
            log.w { "Service already running, skipping start call" }
            return
        }
        log.d { "Starting notification service in JS (stub implementation)" }
        isRunning = true
    }

    actual override fun stopService() {
        if (!isRunning) {
            log.w { "Service is not running, skipping stop call" }
            return
        }
        log.d { "Stopping notification service in JS (stub implementation)" }
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

    actual override fun isServiceRunning(): Boolean {
        return isRunning
    }

    actual fun pushNotification(title: String, message: String) {
        if (isAppInForeground()) {
            log.w { "Skipping notification since app is in the foreground" }
            return
        }

        log.d { "Pushing notification in JS: $title - $message" }
        // In a real implementation, you would use the browser's Notification API
    }

    actual fun isAppInForeground(): Boolean {
        return appForegroundController.isForeground.value
    }
}