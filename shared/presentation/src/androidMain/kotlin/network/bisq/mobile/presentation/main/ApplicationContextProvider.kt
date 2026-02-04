package network.bisq.mobile.presentation.main

import android.annotation.SuppressLint
import android.content.Context

/**
 * Provides global access to the Application context.
 * This is initialized in MainApplication.onCreate() and can be used
 * by code that needs context but cannot use Koin injection (e.g., expect/actual functions).
 */
@SuppressLint("StaticFieldLeak")
object ApplicationContextProvider {
    private var _context: Context? = null

    val context: Context
        get() =
            _context ?: throw IllegalStateException(
                "ApplicationContextProvider not initialized. Call initialize() in Application.onCreate()",
            )

    fun initialize(context: Context) {
        _context = context.applicationContext
    }

    /**
     * Resets the context to null. Only for use in tests to avoid test pollution.
     */
    @androidx.annotation.VisibleForTesting
    fun reset() {
        _context = null
    }
}
