package network.bisq.mobile.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.utils.Logging

/**
 * JS-specific implementation of AppForegroundController
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppForegroundController : ForegroundDetector, Logging {
    private val _isForeground = MutableStateFlow(true)
    override val isForeground: StateFlow<Boolean> = _isForeground

    init {
        // Listen for visibility change events in the browser
        js("""
            document.addEventListener('visibilitychange', function() {
                if (document.hidden) {
                    this.onAppDidEnterBackground();
                } else {
                    this.onAppWillEnterForeground();
                }
            }.bind(this));
        """)
    }

    private fun onAppDidEnterBackground() {
        log.d { "App is in foreground -> false" }
        _isForeground.value = false
    }

    private fun onAppWillEnterForeground() {
        log.d { "App is in foreground -> true" }
        _isForeground.value = true
    }
}