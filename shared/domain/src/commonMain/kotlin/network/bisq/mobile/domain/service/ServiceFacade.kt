package network.bisq.mobile.domain.service

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import org.koin.core.component.KoinComponent
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
 * - Launch coroutines via `launchIO()` or `collectIO()`
 * - Call `deactivate()` to cancel all coroutines and release resources
 */
abstract class ServiceFacade : LifeCycleAware, KoinComponent, Logging {
    private var isActivated = atomic(false)
    
    // we use KoinCompoent inject to avoid having to pass the manager as parameter on every single service
    protected val jobsManager: CoroutineJobsManager by inject()
    
    // Provide access to the IO scope from the jobsManager
    protected val serviceScope: CoroutineScope
        get() = jobsManager.getIOScope()

    override fun activate() {
        require(!isActivated.value) { "activate called on ${this::class.simpleName} while service is already activated" }

        log.i { "${this::class.simpleName} activated" }
        isActivated.value = true
    }

    override fun deactivate() {
        if (isActivated.compareAndSet(expect = true, update = false)) {
            log.i { "Deactivating service ${this::class.simpleName}" }
            
            // Clean up all jobs managed by the jobsManager
            CoroutineScope(IODispatcher).launch {
                jobsManager.dispose()
            }
        }
    }
    
    // Helper methods for launching coroutines with the jobsManager
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return jobsManager.launchIO(block)
    }
    
    protected fun <T> collectIO(flow: Flow<T>, collector: suspend (T) -> Unit): Job {
        return jobsManager.collectIO(flow, collector)
    }
}
