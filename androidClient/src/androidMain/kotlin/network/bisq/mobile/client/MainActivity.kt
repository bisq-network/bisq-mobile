package network.bisq.mobile.client

import network.bisq.mobile.presentation.BisqMainActivity

/**
 * Android Bisq Connect Main Activity
 */
class MainActivity : BisqMainActivity() {
    
    override fun setupKoinDI() {
        MainApplication.setupKoinDI(applicationContext)
    }
}