package network.bisq.mobile.client

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.BisqMainActivity

/**
 * Android Bisq Connect Main Activity
 */
class MainActivity : BisqMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }
    
    override fun setupKoinDI() {
        MainApplication.setupKoinDI(applicationContext)
    }
}