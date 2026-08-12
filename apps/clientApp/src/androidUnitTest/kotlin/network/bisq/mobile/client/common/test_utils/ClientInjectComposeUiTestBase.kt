package network.bisq.mobile.client.common.test_utils

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.After
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException

/**
 * Leaf base for client Compose UI tests that need per-test Koin inject overrides.
 *
 * Owns a single `startKoin` graph (`clientTestModule` + [additionalModules]) on a plain
 * [Application] — do **not** pair with [TestApplication]. Prefer
 * [BisqComposeUiTestBase] + `@Config(TestApplication)` when inject overrides are unnecessary.
 * Proof: `ClientSplashScreenUiTest`.
 */
@Config(application = Application::class)
abstract class ClientInjectComposeUiTestBase : BisqComposeUiTestBase() {
    protected lateinit var koinApplication: KoinApplication
    protected lateinit var viewModelStore: ViewModelStore
    protected lateinit var viewModelStoreOwner: ViewModelStoreOwner

    /** Extra modules merged after [clientTestModule]. Build mocks in [onBeforeKoinStart] first. */
    protected open fun additionalModules(): List<Module> = emptyList()

    /** Create mocks / state captured by [additionalModules] definitions. */
    protected open fun onBeforeKoinStart() {}

    override fun setUpUiTest() {
        super.setUpUiTest()
        viewModelStore = ViewModelStore()
        viewModelStoreOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = this@ClientInjectComposeUiTestBase.viewModelStore
            }
        onBeforeKoinStart()
        runCatching { stopKoin() }.onFailure { if (it is CancellationException) throw it }
        koinApplication =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext())
                modules(listOf(clientTestModule) + additionalModules())
            }
    }

    @After
    fun tearDownInjectCompose() {
        runCatching {
            composeTestRule.waitForIdle()
        }.onFailure { if (it is CancellationException) throw it }
        runCatching { viewModelStore.clear() }.onFailure { if (it is CancellationException) throw it }
        runCatching { stopKoin() }.onFailure { if (it is CancellationException) throw it }
    }

    /**
     * Sets content under the owned Koin graph and a fresh [LocalViewModelStoreOwner] so
     * BackStackAware presenters do not reuse Activity-scoped ViewModels across tests.
     */
    protected fun setInjectTestContent(content: @Composable () -> Unit) {
        setTestContent {
            KoinIsolatedContext(koinApplication) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    content()
                }
            }
        }
    }
}
