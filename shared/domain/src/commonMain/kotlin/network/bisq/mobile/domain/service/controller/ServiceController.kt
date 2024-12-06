package network.bisq.mobile.domain.service.controller

/**
 * Service controller behaviour definitions
 */
interface ServiceController {
    fun startService()
    fun stopService()
    fun isServiceRunning(): Boolean
}