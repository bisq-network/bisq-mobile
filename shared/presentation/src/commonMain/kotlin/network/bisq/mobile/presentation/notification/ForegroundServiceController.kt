package network.bisq.mobile.presentation.notification

import kotlinx.coroutines.flow.StateFlow

/**
 * An interface for a controller of a notification service
 */
interface ForegroundServiceController {
    fun startService()
    fun stopService()
    fun <T> registerObserver(stateFlow: StateFlow<T>, onStateChange: (T) -> Unit)
    fun unregisterObserver(stateFlow: StateFlow<*>)
    fun isServiceRunning(): Boolean
}