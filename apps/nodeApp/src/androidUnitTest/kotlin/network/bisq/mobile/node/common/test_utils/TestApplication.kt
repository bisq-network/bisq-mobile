package network.bisq.mobile.node.common.test_utils

import android.app.Application
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.node.common.di.testModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Test Application for Robolectric UI tests.
 *
 * Replaces the real node Application (via `@Config(application = TestApplication::class)`) so
 * Robolectric does not boot the heavy bisq2 stack / `AndroidAppContext`. Initializes i18n and a
 * minimal Koin context for composables that resolve dependencies.
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        I18nSupport.setLanguage()

        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }

        startKoin {
            androidContext(this@TestApplication)
            modules(testModule)
        }
    }
}
