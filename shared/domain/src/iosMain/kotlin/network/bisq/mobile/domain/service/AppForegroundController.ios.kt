package network.bisq.mobile.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.utils.Logging
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppForegroundController : ForegroundDetector, Logging {
    private val _isForeground = MutableStateFlow(true)
    override val isForeground: StateFlow<Boolean> = _isForeground

    init {
        val notificationCenter = NSNotificationCenter.defaultCenter
        notificationCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = null
        ) { notification ->
            onAppDidEnterBackground()
        }
        notificationCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = null
        ) { notification ->
            onAppWillEnterForeground()
        }
    }

    private fun onAppDidEnterBackground() {
        log.d {"App is in foreground -> false" }
        _isForeground.value = false

    }

    private fun onAppWillEnterForeground() {
        log.d {"App is in foreground -> true" }
        _isForeground.value = true
    }
}