package network.bisq.mobile.domain.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.concurrent.Volatile
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Interface for managing coroutine jobs with lifecycle awareness.
 * This helps centralize job management and disposal across the application.
 */
interface CoroutineJobsManager {
    /**
     * Dispose all managed jobs and recreate scope
     */
    suspend fun dispose()

    /**
     * Get the coroutine scope. prefers Dispatchers.Main.immediate (or Dispatchers.Main), falling back to a platform-safe context when Main is unavailable
     */
    fun getScope(): CoroutineScope

    /**
     * Set a custom coroutine exception handler.
     */
    var coroutineExceptionHandler: ((Throwable) -> Unit)?
}

/**
 * Implementation of [CoroutineJobsManager] that manages coroutine jobs and their lifecycle.
 */
class DefaultCoroutineJobsManager :
    CoroutineJobsManager,
    Logging {
    @Volatile
    override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null

    private val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            log.e(exception) { "Uncaught coroutine exception" }
            // Handle the exception gracefully
            try {
                coroutineExceptionHandler?.invoke(exception)
            } catch (e: Exception) {
                log.e(e) { "Error in coroutine exception handler" }
            }
        }

    private var scope: CoroutineScope = createScope()

    override fun getScope() = scope

    override suspend fun dispose() {
        // JobsManager is currently getting disposed on BasePresenter on unattach (screen's composable onDispose),
        // Which is different from how viewModelScope supposed to work
        runCatching { scope.cancel() }.onFailure { throwable ->
            log.w(throwable) { "Failed to cancel scope: ${throwable.message}" }
        }
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
        return CoroutineScope(dispatcher + SupervisorJob() + exceptionHandler)
    }

    private fun createDispatcher(): MainCoroutineDispatcher =
        try {
            Dispatchers.Main.immediate
        } catch (_: UnsupportedOperationException) {
            Dispatchers.Main
        }
}
