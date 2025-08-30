package network.bisq.mobile.android.node

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.BisqMainActivity

/**
 * Bisq Android Node Main Activity
 */
class MainActivity : BisqMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate to prevent UI blocking
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Enforce enable hardware acceleration for better graphics performance
        // tested with better results than manifest flag
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
    }

    override fun setupKoinDI() {
        // this is needed here to ensure cleanups in "zombie state"
        MainApplication.setupKoinDI(applicationContext)
    }
}