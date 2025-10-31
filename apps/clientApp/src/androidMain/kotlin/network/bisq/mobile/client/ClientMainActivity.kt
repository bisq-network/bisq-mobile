package network.bisq.mobile.client

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.MainActivity

/**
 * Android Bisq Connect Main Activity
 */
class ClientMainActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }
}