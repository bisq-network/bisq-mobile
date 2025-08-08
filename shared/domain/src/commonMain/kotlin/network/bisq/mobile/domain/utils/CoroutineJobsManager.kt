package network.bisq.mobile.domain.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.getPlatformInfo
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Interface for managing coroutine jobs with lifecycle awareness.
 * This helps centralize job management and disposal across the application.
 */
interface CoroutineJobsManager {
    /**
     * Add a job to be managed and automatically disposed when [dispose] is called.
     * @param job The job to be managed
     * @return The same job for chaining
     */
    fun addJob(job: Job): Job
    
    /**
     * Launch a new coroutine in the UI scope and automatically manage its lifecycle.
     * @param block The coroutine code to execute
     * @return The created job
     */
    fun launchUI(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit): Job
    
    /**
     * Launch a new coroutine in the IO scope and automatically manage its lifecycle.
     * @param block The coroutine code to execute
     * @return The created job
     */
    fun launchIO(block: suspend CoroutineScope.() -> Unit): Job
    
    /**
     * Collect a flow in the UI scope and automatically manage the job's lifecycle.
     * @param flow The flow to collect
     * @param collector The collector function
     * @return The created job
     */
    fun <T> collectUI(flow: Flow<T>, collector: suspend (T) -> Unit): Job
    
    /**
     * Collect a flow in the IO scope and automatically manage the job's lifecycle.
     * @param flow The flow to collect
     * @param collector The collector function
     * @return The created job
     */
    fun <T> collectIO(flow: Flow<T>, collector: suspend (T) -> Unit): Job
    
    /**
     * Dispose all managed jobs.
     */
    suspend fun dispose()
    
    /**
     * Get the UI coroutine scope.
     */
    fun getUIScope(): CoroutineScope
    
    /**
     * Get the IO coroutine scope.
     */
    fun getIOScope(): CoroutineScope

    /**
     * Set a custom coroutine exception handler.
     * Note: On iOS, this method has no effect due to platform limitations.
     * @param handler The exception handler callback
     */
    fun setCoroutineExceptionHandler(handler: (Throwable) -> Unit)
}

/**
 * Implementation of [CoroutineJobsManager] that manages coroutine jobs and their lifecycle.
 */
class DefaultCoroutineJobsManager : CoroutineJobsManager, Logging {
    private val jobs = mutableSetOf<Job>()
    private val jobsMutex = Mutex()

    // Dedicated scope for job management operations - independent of user scopes
    private var jobManagementScope = CoroutineScope(IODispatcher + SupervisorJob())

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        log.e(exception) { "Uncaught coroutine exception" }

        // Handle the exception gracefully
        try {
            onCoroutineException?.invoke(exception)
        } catch (e: Exception) {
            log.e(e) { "Error in coroutine exception handler" }
        }
    }

    // TODO we might need to make the whole manager platform-specific to cater for iOS properly
    // Platform-aware scope creation
    private val isIOS = getPlatformInfo().name.lowercase().contains("ios")
    private var uiScope = if (isIOS) {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    } else {
        CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)
    }
    private var ioScope = if (isIOS) {
        CoroutineScope(IODispatcher + SupervisorJob())
    } else {
        CoroutineScope(IODispatcher + SupervisorJob() + exceptionHandler)
    }

    // Callback for handling coroutine exceptions
    private var onCoroutineException: ((Throwable) -> Unit)? = null

    override fun setCoroutineExceptionHandler(handler: (Throwable) -> Unit) {
        if (isIOS) {
            log.d { "iOS detected - coroutine exception handler not supported" }
            return
        }
        onCoroutineException = handler
    }
    
    // uses dedicated scope to avoid collisions
    override fun addJob(job: Job): Job {
        jobManagementScope.launch {
            jobsMutex.withLock { jobs.add(job) }
        }
        job.invokeOnCompletion {
            jobManagementScope.launch {
                jobsMutex.withLock { jobs.remove(job) }
            }
        }
        return job
    }
    
    override fun launchUI(context: CoroutineContext,  block: suspend CoroutineScope.() -> Unit): Job {
        return addJob(uiScope.launch(context) { block() })
    }

    override fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return addJob(ioScope.launch { block() })
    }
    
    override fun <T> collectUI(flow: Flow<T>, collector: suspend (T) -> Unit): Job {
        return launchUI {
            flow.collect { collector(it) }
        }
    }
    
    override fun <T> collectIO(flow: Flow<T>, collector: suspend (T) -> Unit): Job {
        return launchIO {
            flow.collect { collector(it) }
        }
    }

    override fun getUIScope(): CoroutineScope = uiScope

    override fun getIOScope(): CoroutineScope = ioScope
    
    override suspend fun dispose() {
        if (isIOS) {
            // On iOS, don't dispose scopes during shutdown to avoid crashes
            // The system will clean up when the app terminates
            return
        }

        // Android - normal disposal
        disposeScopes()
        recreateScopes()
    }

    private suspend fun disposeScopes() {
        runCatching {
            jobsMutex.withLock {
                jobs.forEach {
                    runCatching { it.cancel() }.onFailure {
                        log.w { "Failed to cancel job: ${it.message}" }
                    }
                }
                jobs.clear()
            }

            runCatching { uiScope.cancel() }.onFailure {
                log.w { "Failed to cancel UI scope: ${it.message}" }
            }
            runCatching { ioScope.cancel() }.onFailure {
                log.w { "Failed to cancel IO scope: ${it.message}" }
            }
            runCatching { jobManagementScope.cancel() }.onFailure {
                log.w { "Failed to cancel job management scope: ${it.message}" }
            }
        }.onFailure {
            log.e(it) { "Failed to dispose coroutine jobs" }
        }
    }

    private fun recreateScopes() {
        runCatching {
            uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)
            ioScope = CoroutineScope(IODispatcher + SupervisorJob() + exceptionHandler)
            jobManagementScope = CoroutineScope(IODispatcher + SupervisorJob())
        }.onFailure {
            log.e(it) { "Failed to recreate coroutine scopes" }
            // Fallback: create basic scopes without exception handler
            uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            ioScope = CoroutineScope(IODispatcher + SupervisorJob())
            jobManagementScope = CoroutineScope(IODispatcher + SupervisorJob())
        }
    }
}