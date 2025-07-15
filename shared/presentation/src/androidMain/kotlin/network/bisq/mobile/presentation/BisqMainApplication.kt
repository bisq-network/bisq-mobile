package network.bisq.mobile.presentation

import android.app.Application
import network.bisq.mobile.domain.utils.Logging

/**
 * Base class for Bisq Android Applications
 */
abstract class BisqMainApplication : Application(), Logging {
    
    override fun onCreate() {
        super.onCreate()
        setupKoinDI()
        onCreated()
    }
    
    protected abstract fun setupKoinDI()

    protected open fun onCreated() {
        // default impl
    }
} 