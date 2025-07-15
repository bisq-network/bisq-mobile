package network.bisq.mobile.android.node

import network.bisq.mobile.presentation.BisqMainActivity

/**
 * Bisq Android Node Main Activity
 */
class MainActivity : BisqMainActivity() {
    
    override fun setupKoinDI() {
        // this is needed here to ensure cleanups in "zombie state"
        MainApplication.setupKoinDI(applicationContext)
    }
}