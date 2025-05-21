package network.bisq.mobile.domain.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.IODispatcher

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
    fun launchUI(block: suspend CoroutineScope.() -> Unit): Job
    
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
}

/**
 * Implementation of [CoroutineJobsManager] that manages coroutine jobs and their lifecycle.
 */
class DefaultCoroutineJobsManager : CoroutineJobsManager, Logging {
    private val jobs = mutableSetOf<Job>()
    private val jobsMutex = Mutex()
    private var uiScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main + SupervisorJob())
    private var ioScope = CoroutineScope(IODispatcher + SupervisorJob())
    
    override fun addJob(job: Job): Job {
        // direct, lock-protected mutation – no extra coroutines
        runBlocking {           // cheap; single quick critical section
            jobsMutex.withLock { jobs.add(job) }
        }

        job.invokeOnCompletion {
            runBlocking {
                jobsMutex.withLock { jobs.remove(job) }
            }
        }
        return job
    }
    
    override fun launchUI(block: suspend CoroutineScope.() -> Unit): Job {
        return addJob(uiScope.launch { block() })
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
    
    override suspend fun dispose() {
        log.d { "Disposing coroutine jobs" }

        jobsMutex.withLock {
            log.d { "Disposing ${jobs.size} coroutine jobs" }
            jobs.forEach { it.cancel() }
            jobs.clear()
        }

        uiScope.cancel()
        ioScope.cancel()
        uiScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main + SupervisorJob())
        ioScope = CoroutineScope(IODispatcher + SupervisorJob())
    }
    
    override fun getUIScope(): CoroutineScope = uiScope
    
    override fun getIOScope(): CoroutineScope = ioScope
}