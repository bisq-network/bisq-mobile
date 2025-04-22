package network.bisq.mobile.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.Logging

/**
 * Base class for lifecycle-aware service components that require coroutine-based background execution.
 *
 * `ServiceFacade` provides a lazily-initialized `CoroutineScope` (`serviceScope`) tied to a `SupervisorJob`,
 * which allows safe concurrent coroutine execution where failures in one child coroutine do not cancel others.
 *
 * The scope is only created on first use (lazy initialization) and is automatically reset if the underlying job is cancelled.
 *
 * The `deactivate()` method cancels the current service scope and its associated job, ensuring a clean shutdown
 * of all running coroutines. Subclasses can override `activate()` to start background work as needed.
 *
 * Typical usage pattern:
 * - Call `activate()` when the service is started (optionally overridden by subclasses)
 * - Launch coroutines via `serviceScope`
 * - Call `deactivate()` to cancel all coroutines and release resources
 */
abstract class ServiceFacade : LifeCycleAware, Logging {
    private var _serviceJob: Job? = null
    private var _serviceScope: CoroutineScope? = null
    private var isActivated = false

    protected val serviceScope: CoroutineScope
        get() {
            if (_serviceScope == null || _serviceJob?.isCancelled == true) {
                _serviceJob = SupervisorJob()
                _serviceScope = CoroutineScope(IODispatcher + _serviceJob!!)
            }
            return _serviceScope!!
        }

    override fun activate() {
        require(!isActivated) { "activate called on ${this::class.simpleName} while service is already activated" }

        log.i { "${this::class.simpleName} activated" }
        isActivated = true
    }

    override fun deactivate() {
        if (!isActivated) {
            "deactivate called on ${this::class.simpleName} while service is not activated. " +
                    "This might be a valid case if we shut down fast before service got activated."
        }
        _serviceScope?.cancel()
        _serviceJob = null
        _serviceScope = null
        isActivated = false

        log.i { "${this::class.simpleName} deactivates" }
    }
}
