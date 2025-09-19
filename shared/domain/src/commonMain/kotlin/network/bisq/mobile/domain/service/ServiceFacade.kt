package network.bisq.mobile.domain.service

import androidx.annotation.CallSuper
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.IODispatcher

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
 * - Launch coroutines via `launchIO()` or `collectIO()`
 * - Call `deactivate()` to cancel all coroutines and release resources
 *
 */
abstract class ServiceFacade : BaseService(), LifeCycleAware {

    private var isActivated = atomic(false)

    @CallSuper
    override fun activate() {
        require(!isActivated.value) { "activate called on ${this::class.simpleName} while service is already activated" }

        log.i { "${this::class.simpleName} activated" }
        isActivated.value = true
    }

    @CallSuper
    override fun deactivate() {
        if (isActivated.compareAndSet(expect = true, update = false)) {
            log.i { "Deactivating service ${this::class.simpleName}" }

            // Clean up all jobs managed by the jobsManager in a new scope because this will be killed
            CoroutineScope(IODispatcher).launch {
                jobsManager.dispose()
            }
        }
    }

    protected fun reactivate() {
        deactivate()
        activate()
    }
}
