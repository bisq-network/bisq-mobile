package network.bisq.mobile.android.node

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import network.bisq.mobile.presentation.MainActivity

/**
 * Bisq Android Node Main Activity
 */
class NodeMainActivity : MainActivity() {

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
}