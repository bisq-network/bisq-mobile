package network.bisq.mobile.client.common.test_utils

import android.app.Application
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.i18n.I18nSupport
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Test Application for Robolectric UI tests.
 * Initializes Koin with test module for UI tests that need dependencies like TopBar.
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize i18n for UI text testing
        I18nSupport.setLanguage()

        // Stop Koin if already started (from previous test)
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }

        // Initialize Koin with test module
        startKoin {
            androidContext(this@TestApplication)
            modules(clientTestModule)
        }
    }
}
