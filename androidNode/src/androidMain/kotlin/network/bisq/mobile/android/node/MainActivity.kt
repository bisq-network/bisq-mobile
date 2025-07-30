package network.bisq.mobile.android.node

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.BisqMainActivity

/**
 * Bisq Android Node Main Activity
 */
class MainActivity : BisqMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }
    
    override fun setupKoinDI() {
        // this is needed here to ensure cleanups in "zombie state"
        MainApplication.setupKoinDI(applicationContext)
    }
}