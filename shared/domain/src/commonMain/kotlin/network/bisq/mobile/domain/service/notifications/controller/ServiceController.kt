package network.bisq.mobile.domain.service.notifications.controller

import kotlinx.coroutines.flow.StateFlow

/**
 * Service controller behaviour definitions
 */
interface ServiceController {
    fun startService()
    fun stopService()
    fun <T> registerObserver(stateFlow: StateFlow<T>, onStateChange: (T) -> Unit)
    fun unregisterObserver(stateFlow: StateFlow<*>)
    fun isServiceRunning(): Boolean
}