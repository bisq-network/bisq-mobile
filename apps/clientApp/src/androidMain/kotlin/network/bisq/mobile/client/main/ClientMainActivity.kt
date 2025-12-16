package network.bisq.mobile.client.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.main.MainActivity

/**
 * Android Bisq Connect Main Activity
 */
class ClientMainActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            ClientApp()
        }
    }
}