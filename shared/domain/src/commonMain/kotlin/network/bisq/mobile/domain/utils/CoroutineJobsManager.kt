package network.bisq.mobile.domain.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Interface for managing coroutine jobs with lifecycle awareness.
 * This helps centralize job management and disposal across the application.
 */
interface CoroutineJobsManager {
    /**
     * Dispose all managed jobs.
     */
    suspend fun dispose()
    
    /**
     * Get the coroutine scope. prefers Dispatchers.Main.immediate (or Dispatchers.Main), falling back to a platform-safe context when Main is unavailable
     */
    fun getScope(): CoroutineScope

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
    private val isIOS = getPlatformInfo().type == PlatformType.IOS


    private var scope: CoroutineScope = createScope()

    // Callback for handling coroutine exceptions
    private var onCoroutineException: ((Throwable) -> Unit)? = null

    override fun setCoroutineExceptionHandler(handler: (Throwable) -> Unit) {
        if (isIOS) {
            log.d { "iOS detected - coroutine exception handler not supported" }
            return
        }
        onCoroutineException = handler
    }

    override fun getScope(): CoroutineScope = scope

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

    private fun disposeScopes() {
        runCatching { scope.cancel() }.onFailure { throwable ->
            log.w(throwable) { "Failed to cancel scope: ${throwable.message}" }
        }
    }

    private fun recreateScopes() {
        scope = createScope()
    }

    private fun createScope(): CoroutineScope {
        // this is how viewModelScope dispatcher is selected:
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:lifecycle/lifecycle-viewmodel/src/commonMain/kotlin/androidx/lifecycle/viewmodel/internal/CloseableCoroutineScope.kt;l=52-69
        val dispatcher =
            try {
                // In platforms where `Dispatchers.Main` is not available, Kotlin Multiplatform will
                // throw
                // an exception (the specific exception type may depend on the platform). Since there's
                // no
                // direct functional alternative, we use `EmptyCoroutineContext` to ensure that a
                // coroutine
                // launched within this scope will run in the same context as the caller.
                createDispatcher()
            } catch (_: NotImplementedError) {
                // In Native environments where `Dispatchers.Main` might not exist (e.g., Linux):
                EmptyCoroutineContext
            } catch (_: IllegalStateException) {
                // In JVM Desktop environments where `Dispatchers.Main` might not exist (e.g., Swing):
                EmptyCoroutineContext
            }
        return if (isIOS) {
            CoroutineScope(dispatcher + SupervisorJob())
        } else {
            CoroutineScope(dispatcher + SupervisorJob() + exceptionHandler)
        }
    }

    private fun createDispatcher(): MainCoroutineDispatcher {
        return try {
            Dispatchers.Main.immediate
        } catch (_: UnsupportedOperationException) {
            Dispatchers.Main
        }
    }
}