package network.bisq.mobile.client

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.MainActivity
import network.bisq.mobile.presentation.MainApplication

/**
 * Android Bisq Connect Main Activity
 */
class ClientMainActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        if (MainApplication.wasProcessDead.getAndSet(false)) {
            // this is to enforce proper initialization if process was killed by OS
            super.onCreate(null)
        } else {
            super.onCreate(savedInstanceState)
        }
    }
}