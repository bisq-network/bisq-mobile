package network.bisq.mobile.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.utils.Logging

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppForegroundController : ForegroundDetector, Logging {
    private val _isForeground = MutableStateFlow(true)
    override val isForeground: StateFlow<Boolean> = _isForeground

    init {
        // TODO For JS, we can use the document visibility API to detect foreground/background
        // stub impl:
        log.d { "Initializing JS AppForegroundController" }
        setupVisibilityListener()
    }

    private fun setupVisibilityListener() {
        // TODO implement JS interop
        // For now, we'll just assume the app is always in foreground for JS
        _isForeground.value = true
    }
}