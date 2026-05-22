package network.bisq.mobile.presentation.common.ui.error

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.utils.setupUncaughtExceptionHandler
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.getLogger
import org.koin.mp.KoinPlatformTools
import kotlin.jvm.JvmStatic

class GenericErrorHandler : Logging {
    companion object {
        private val _isUncaughtException: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val isUncaughtException: StateFlow<Boolean> = _isUncaughtException.asStateFlow()

        private val _genericErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
        val genericErrorMessage: StateFlow<String?> = _genericErrorMessage.asStateFlow()

        fun clearGenericError() {
            _isUncaughtException.value = false
            _genericErrorMessage.value = null
        }

        fun handleGenericError(value: String?) {
            _genericErrorMessage.value = value
        }

        fun handleGenericError(
            errorMessage: String,
            exception: Throwable,
        ) {
            getLogger("GenericErrorHandler").e(errorMessage, exception)
            // Forward to analytics: this overload is the catch-all for handled
            // exceptions across the codebase (Compose lifecycle helpers,
            // facade error paths, etc.). Without this call, anything that
            // reaches us through `handleGenericError(message, throwable)` is
            // logged + surfaced in UI but never sent to GlitchTip — i.e. only
            // truly-uncaught JVM-level crashes would ever reach Sentry. Adding
            // it here makes the handled-but-noteworthy class of errors visible
            // too. SDK dedupes by event_id so this is safe vs. Sentry's own
            // auto-handler.
            forwardToAnalytics(exception)
            handleGenericError(errorMessage + "\nException: " + exception.message)
        }

        /**
         * Forward a throwable to the analytics service if one is registered in
         * Koin. Looked up lazily at crash time so the handler is safe to call
         * before, during, or after DI startup — a missing binding silently
         * no-ops rather than masking the original crash.
         *
         * Belt-and-braces for two reasons:
         *  1. Sentry-KMP auto-installs its own `UncaughtExceptionHandler` on
         *     init; this would catch the same crashes regardless. Calling
         *     `captureException` explicitly here is idempotent at the SDK level
         *     (the SDK dedupes by event_id) and protects against init ordering
         *     where Sentry's handler ends up below ours in the chain.
         *  2. Coroutine failures bypass the JVM-level uncaught handler entirely
         *     — they go through `CoroutineExceptionHandler`. The hook in
         *     [setupCoroutineExceptionHandler] below is the only path that
         *     gets those into analytics at all.
         */
        private fun forwardToAnalytics(throwable: Throwable) {
            runCatching {
                KoinPlatformTools
                    .defaultContext()
                    .getOrNull()
                    ?.get<AnalyticsService>()
                    ?.captureException(throwable)
            }
        }

        @JvmStatic
        fun init() {
            // Set up uncaught exception handler
            setupUncaughtExceptionHandler { throwable ->
                _isUncaughtException.value = true
                forwardToAnalytics(throwable)
                // this ensures compose can observe the change.
                MainScope().launch {
                    handleGenericError("Application stopped unexpectedly", throwable)
                }
            }
        }

        @JvmStatic
        fun setupCoroutineExceptionHandler(handlerSetup: CoroutineExceptionHandlerSetup) {
            handlerSetup.setGlobalExceptionHandler { throwable ->
                _isUncaughtException.value = false // This is a handled coroutine exception, not uncaught
                forwardToAnalytics(throwable)
                MainScope().launch {
                    handleGenericError("Coroutine operation failed", throwable)
                }
            }
        }
    }
}
