package network.bisq.mobile.presentation.common.notification

import kotlinx.coroutines.flow.Flow

/**
 * An interface for a controller of a notification service
 */
interface ForegroundServiceController {
    fun startService()

    fun stopService()

    /**
     * Re-posts the running service's foreground notification. Needed on Android after the
     * user grants POST_NOTIFICATIONS while the service is already running: the notification
     * posted pre-grant was silently dropped by the OS and is never retro-displayed, leaving
     * the service invisible in the status bar (issue #1749). No-op if the service isn't
     * running, and on platforms without a persistent service notification.
     */
    fun refreshNotification()

    fun <T> registerObserver(
        flow: Flow<T>,
        onStateChange: suspend (T) -> Unit,
    )

    fun unregisterObserver(flow: Flow<*>)

    fun unregisterObservers()

    fun isServiceRunning(): Boolean

    fun dispose()
}
