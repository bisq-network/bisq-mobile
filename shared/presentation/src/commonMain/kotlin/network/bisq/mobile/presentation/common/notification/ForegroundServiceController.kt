package network.bisq.mobile.presentation.common.notification

import kotlinx.coroutines.flow.Flow

/**
 * An interface for a controller of a notification service
 */
interface ForegroundServiceController {
    fun startService()
    fun stopService()
    fun <T> registerObserver(flow: Flow<T>, onStateChange: (T) -> Unit)
    fun unregisterObserver(flow: Flow<*>)
    fun unregisterObservers()
    fun isServiceRunning(): Boolean
    fun dispose()
}