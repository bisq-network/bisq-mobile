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
    private var _context: Context? = null

    val context: Context
        get() =
            _context
                ?: throw IllegalStateException(
                    "AndroidAppContext not initialized. Call initialize() in Application.onCreate().",
                )

    fun initialize(context: Context) {
        _context = context.applicationContext
    }

    @VisibleForTesting
    fun reset() {
        _context = null
    }
}
