package network.bisq.mobile.domain.service

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.Logging

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppForegroundController(val context: Context) : ForegroundDetector, Logging {
    private val _isForeground = MutableStateFlow(false)
    override val isForeground: StateFlow<Boolean> get() = _isForeground.asStateFlow()

    private var startedCount: Int = 0

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            // Use Started/Stopped to track foreground reliably across transient pauses
            override fun onActivityStarted(activity: Activity) {
                startedCount += 1
                if (startedCount == 1) {
                    onAppEnteredForeground()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount -= 1
                if (startedCount <= 0) {
                    startedCount = 0
                    onAppEnteredBackground()
                }
            }

            // Other lifecycle methods can be left empty
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
    private fun onAppEnteredBackground() {
        log.d("App is in foreground -> false")
        _isForeground.value = false
    }

    private fun onAppEnteredForeground() {
        log.d("App is in foreground -> true")
        _isForeground.value = true
    }

}