package network.bisq.mobile.test.koin

import network.bisq.mobile.test.coroutines.CoroutineTestBase
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.test.KoinTest

/**
 * Parameterized Koin test base for Android unit tests.
 *
 * Subclasses an app-specific leaf (e.g. `ClientKoinIntegrationTestBase`) that implements
 * [baseModules] — do not extend this class directly from test cases.
 */
abstract class KoinIntegrationTestBase :
    CoroutineTestBase(),
    KoinTest {
    protected abstract fun baseModules(): List<Module>

    /**
     * Override to provide Koin modules loaded after [baseModules].
     */
    protected open fun additionalModules(): List<Module> = emptyList()

    /**
     * Called after [setUpCoroutines] and before Koin starts. Use to create mocks that must
     * exist before modules are loaded (e.g. navigation or UI managers passed into test modules).
     */
    protected open fun beforeStartKoin() {}

    protected open fun onSetup() {}

    protected open fun onTearDown() {}

    @Before
    fun baseSetup() {
        setUpCoroutines()
        beforeStartKoin()
        startKoin {
            modules(baseModules())
            modules(additionalModules())
        }
        onSetup()
    }

    @After
    fun baseTearDown() {
        onTearDown()
        stopKoin()
        tearDownCoroutines()
    }
}
