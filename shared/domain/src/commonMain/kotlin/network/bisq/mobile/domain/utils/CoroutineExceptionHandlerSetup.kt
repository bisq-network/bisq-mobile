package network.bisq.mobile.domain.utils

/**
 * Singleton class responsible for setting up coroutine exception handlers
 * across all CoroutineJobsManager instances in the application.
 * 
 * This ensures consistent exception handling while allowing each component
 * to have its own isolated job management.
 */
class CoroutineExceptionHandlerSetup {
    
    private var exceptionHandler: ((Throwable) -> Unit)? = null
    
    /**
     * Set the global exception handler that will be applied to all
     * CoroutineJobsManager instances.
     */
    fun setGlobalExceptionHandler(handler: (Throwable) -> Unit) {
        exceptionHandler = handler
    }
    
    /**
     * Apply the global exception handler to a CoroutineJobsManager instance.
     * This is called automatically when creating new job managers.
     */
    fun setupExceptionHandler(jobsManager: CoroutineJobsManager) {
        exceptionHandler?.let { handler ->
            jobsManager.setCoroutineExceptionHandler(handler)
        }
    }
}
