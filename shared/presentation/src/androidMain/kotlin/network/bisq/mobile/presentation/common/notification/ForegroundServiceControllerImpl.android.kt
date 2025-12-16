package network.bisq.mobile.presentation.common.notification

import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging

/**
 * Controller interacting with the bisq foreground service
 */
class ForegroundServiceControllerImpl(
    private val appForegroundController: AppForegroundController,
) : ForegroundServiceController, Logging {
    private val context get() = appForegroundController.context

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observerJobs = mutableMapOf<Flow<*>, Job>()

    @Volatile
    private var isRunning = false

    /**
     * Starts the service in the appropriate mode based on the current device running Android API
     */
    override fun startService() {
        if (isRunning) {
            log.w { "Service already running, skipping start call" }
        } else {
            log.i { "Starting Bisq Service.." }
            val intent = Intent(context, ForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    log.i { "OS supports foreground service" }
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
                log.i { "Started Bisq Service" }
            } catch (e: Exception) {
                isRunning = false
                log.e(e) { "Failed to start ForegroundService" }
            }
        }
    }

    override fun stopService() {
        // TODO if we ever implement Live notifications even if app was killed
        //  we need to leave the service running if the user is ok with it
        if (isRunning) {
            log.i { "Stopping ForegroundService" }
            val intent = Intent(context, ForegroundService::class.java)
            context.stopService(intent)
            isRunning = false
            log.i { "ForegroundService stopped" }
        } else {
            log.i { "ForegroundService is not running, skipping stop call" }
        }
    }

    override fun <T> registerObserver(flow: Flow<T>, onStateChange: (T) -> Unit) {
        if (observerJobs.contains(flow)) {
            log.w { "Flow observer already registered, skipping registration" }
            return
        }
        val job = serviceScope.launch {
            try {
                flow.collect { onStateChange(it) }
            } catch (e: Exception) {
                log.w(e) { "Error in flow observer, flow collection terminated" }
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

    override fun isServiceRunning() = isRunning

    override fun dispose() {
        unregisterObservers()
        serviceScope.cancel()
    }

}
