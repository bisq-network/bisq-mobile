package network.bisq.mobile.data.service

import androidx.annotation.CallSuper
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import org.koin.core.component.inject

/**
 * Base class for lifecycle-aware service components that require coroutine-based background execution.
 *
 * `ServiceFacade` provides coroutine-based background execution through a centralized job management system.
 * All coroutines are launched through the `jobsManager`, which ensures proper lifecycle management and cleanup.
 *
 * The `deactivate()` method ensures a clean shutdown of all running coroutines.
 * Subclasses can override `activate()` to start background work as needed.
 *
 * Typical usage pattern:
 * - Call `activate()` when the service is started (optionally overridden by subclasses)
 * - Launch coroutines via `serviceScope`
 * - Override `clearState()` to drop what the session loaded, so a restart starts from scratch
 * - Derive flows once at construction on `sharingScope`, never on `serviceScope`
 * - Call `deactivate()` to cancel all coroutines and release resources
 *
 */
abstract class ServiceFacade :
    BaseService(),
    LifeCycleAware {
    private var isActivated = atomic(false)

    // Its own manager, never disposed: the one behind serviceScope replaces its scope on every deactivate
    private val sharingJobsManager: CoroutineJobsManager by inject()

    /**
     * Scope for flows derived once at construction (`stateIn` / `shareIn`). Unlike [serviceScope] it is
     * not cancelled on [deactivate], so a derived flow keeps following its sources across a restart
     * instead of freezing on its last value.
     */
    protected val sharingScope: CoroutineScope
        get() = sharingJobsManager.getScope()

    @CallSuper
    override suspend fun activate() {
        require(!isActivated.value) { "activate called on ${this::class.simpleName} while service is already activated" }

        log.i { "${this::class.simpleName} activated" }
        isActivated.value = true
    }

    @CallSuper
    override suspend fun deactivate() {
        // Non-cancellable: a caller cancelled during the join would skip clearState and keep the session
        withContext(NonCancellable) {
            if (isActivated.compareAndSet(expect = true, update = false)) {
                log.i { "Deactivating service ${this@ServiceFacade::class.simpleName}" }

                val scopeJob = serviceScope.coroutineContext[Job]
                jobsManager.dispose()
                // Joined, not just cancelled: a collector applying a payload off main would refill the cleared state
                scopeJob?.join()
            }
            clearState()
        }
    }

    /**
     * Drops the state loaded during the session (from the trusted node or the bisq2 core), so the next
     * [activate] does not show it. Runs on every [deactivate], also for an activation that aborted partway,
     * once the [serviceScope] work has finished, so nothing can refill it. Keep local preferences out of it.
     */
    protected open suspend fun clearState() {}

    /**
     * Test-only helper: cancel all managed coroutines regardless of [isActivated].
     * Used when tests start jobs via [serviceScope] without calling [activate].
     */
    protected suspend fun releaseServiceResources() {
        isActivated.value = false
        jobsManager.dispose()
    }
}
