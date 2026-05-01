package network.bisq.mobile.data.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.VisibleForTesting

/**
 * Domain-level access to the Application context, mirroring
 * `presentation/main/ApplicationContextProvider`. Lives here because
 * `expect`/`actual` functions in `shared/domain/androidMain` cannot reach
 * presentation. Initialized from `MainApplication.onCreate()`.
 */
@SuppressLint("StaticFieldLeak")
object AndroidAppContext {
    // `@Volatile` so the write performed by the main thread in
    // [initialize] is visible to background-thread readers (FCM service
    // thread, IO dispatcher, Koin-owned threads, …). Without this the JMM
    // doesn't guarantee visibility, and a cold start triggered by an FCM
    // push could observe `_context == null` even after main has written.
    @Volatile
    private var _context: Context? = null

    val context: Context
        get() =
            _context
                ?: throw IllegalStateException(
                    "AndroidAppContext not initialized. Call initialize() in Application.onCreate().",
                )

    /**
     * Initializes the Application context. Idempotent for the same context
     * instance. Throws [IllegalStateException] if a different context is
     * passed after initialization — that signals a lifecycle / init-order
     * bug we want to surface immediately rather than silently swap context.
     *
     * `@Synchronized` guarantees the read-then-write is atomic with respect
     * to other writers (e.g. tests calling [reset] between [initialize]
     * calls). Cheap — this method runs once at app startup.
     */
    @Synchronized
    fun initialize(context: Context) {
        val newContext = context.applicationContext
        val current = _context
        when {
            current == null -> _context = newContext
            current === newContext -> Unit // re-init with same context — no-op
            else ->
                error(
                    "AndroidAppContext is already initialized with a different Context. " +
                        "Re-initializing with another instance indicates a lifecycle bug.",
                )
        }
    }

    @VisibleForTesting
    @Synchronized
    fun reset() {
        _context = null
    }
}
