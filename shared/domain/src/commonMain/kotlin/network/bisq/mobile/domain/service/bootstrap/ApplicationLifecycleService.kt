package network.bisq.mobile.domain.service.bootstrap

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.killProcess
import network.bisq.mobile.domain.utils.restartProcess

abstract class ApplicationLifecycleService(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val kmpTorService: KmpTorService,
) : BaseService() {
    private val isTerminating = atomic<Boolean>(false)

    /**
     * Marks the app as terminating if not already started, returns true if this is the first call.
     * Use this as a one-shot guard.
     */
    fun compareAndSetIsTerminating(
        expect: Boolean,
        update: Boolean,
    ): Boolean {
        // this function is to avoid adding atomicfu dependency to node app for now
        return isTerminating.compareAndSet(expect, update)
    }

    fun initialize() {
        log.i { "Initialize core services and Tor" }

        serviceScope.launch {
            try {
                activateServiceFacades()
            } catch (e: Exception) {
                onUnrecoverableError(e)
            }
        }
    }

    protected open fun onUnrecoverableError(e: Throwable) {
        log.e(e) { "Unrecoverable error detected. Application must be restarted. Stopping services." }
        serviceScope.launch {
            try {
                deactivateServiceFacades()
            } catch (e: Exception) {
                log.w(e) { "Error while calling deactivateServiceFacades at onUnrecoverableError. This should not happen." }
            } finally {
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }
        }
    }

    protected abstract suspend fun activateServiceFacades()

    protected abstract suspend fun deactivateServiceFacades()

    fun terminateApp(view: Any?) {
        if (getPlatformInfo().type != PlatformType.ANDROID) return

        if (!compareAndSetIsTerminating(expect = false, update = true)) {
            log.w { "App has already been scheduled for termination; ignoring call to terminateApp." }
            return
        }

        serviceScope.launch {
            try {
                deactivateServiceFacades()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                killProcess(view)
            }
        }
    }

    fun restartApp(
        view: Any?,
        purgeTorDir: Boolean = true,
    ) {
        if (getPlatformInfo().type != PlatformType.ANDROID) return

        if (!compareAndSetIsTerminating(expect = false, update = true)) {
            log.w { "App has already been scheduled for termination; ignoring call to restartApp." }
            return
        }

        serviceScope.launch {
            try {
                // Perform shutdown off the UI thread
                deactivateServiceFacades()
            } catch (e: Exception) {
                log.e("Error at deactivateServiceFacades", e)
            } finally {
                if (purgeTorDir) {
                    // Ensure Tor is fully stopped, wait for control port to close, then purge the Tor dir
                    try {
                        kmpTorService.stopAndPurgeWorkingDir()
                    } catch (e: Exception) {
                        log.w(e) { "Failed to fully stop and purge Tor before restart" }
                    }
                }
                restartProcess(view)
            }
        }
    }
}
