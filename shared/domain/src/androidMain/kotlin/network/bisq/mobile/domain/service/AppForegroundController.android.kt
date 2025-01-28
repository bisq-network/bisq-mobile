package network.bisq.mobile.domain.service

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.utils.Logging

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppForegroundController(val context: Context) : ForegroundDetector, Logging {
    private val _isForeground = MutableStateFlow(false)
    override val isForeground: StateFlow<Boolean> = _isForeground

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                onAppWillEnterForeground()
            }

            override fun onActivityPaused(activity: Activity) {
                onAppDidEnterBackground()
            }

            // Other lifecycle methods can be left empty
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }


    private fun onAppDidEnterBackground() {
        log.d("App is in foreground -> false")
        _isForeground.value = false
    }

    private fun onAppWillEnterForeground() {
        log.d("App is in foreground -> true")
        _isForeground.value = true
    }

}