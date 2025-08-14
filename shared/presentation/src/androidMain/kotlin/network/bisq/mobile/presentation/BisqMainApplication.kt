package network.bisq.mobile.presentation

import android.app.Application
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.SystemOutFilter

/**
 * Base class for Bisq Android Applications
 */
abstract class BisqMainApplication : Application(), Logging {
    
    override fun onCreate() {
        super.onCreate()
        setupSystemOutFiltering()
        setupKoinDI()
        onCreated()
    }
    
    protected abstract fun setupKoinDI()

    protected open fun onCreated() {
        // default impl
    }

    protected open fun isDebug(): Boolean {
        return BuildConfig.IS_DEBUG
    }

    /**
     * Sets up System.out filtering using the shared SystemOutFilter utility.
     * This blocks verbose System.out.println() calls from Bisq2 JARs that bypass the logging framework.
     */
    private fun setupSystemOutFiltering() {
        SystemOutFilter.setupSystemOutFiltering(
            isDebugBuild = isDebug(),
            completeBlockInRelease = true
        )
        log.i { "System.out filtering configured for ${if (isDebug()) "debug" else "release"} build" }
    }
} 