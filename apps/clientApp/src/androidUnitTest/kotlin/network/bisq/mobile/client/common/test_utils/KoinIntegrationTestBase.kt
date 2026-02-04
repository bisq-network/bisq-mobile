package network.bisq.mobile.client.common.test_utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.clientTestModule
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.test.KoinTest

/**
 * Base class for integration tests that require Koin DI.
 *
 * Provides:
 * - Automatic Koin setup/teardown with clientTestModule
 * - Test dispatcher for coroutines
 * - Extension point for additional test modules
 *
 * Usage:
 * ```kotlin
 * class MyIntegrationTest : KoinIntegrationTestBase() {
 *     override fun additionalModules(): List<Module> = listOf(
 *         module {
 *             single<MyDependency> { mockk(relaxed = true) }
 *         }
 *     )
 *
 *     @Test
 *     fun `my test`() = runTest {
 *         // Test code here
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class KoinIntegrationTestBase : KoinTest {
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    /**
     * Override to provide additional Koin modules for the test.
     * These modules are loaded after clientTestModule.
     */
    protected open fun additionalModules(): List<Module> = emptyList()

    /**
     * Override to perform additional setup after Koin is started.
     */
    protected open fun onSetup() {}

    /**
     * Override to perform additional teardown before Koin is stopped.
     */
    protected open fun onTearDown() {}

    @Before
    fun baseSetup() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(clientTestModule)
            modules(additionalModules())
        }
        onSetup()
    }

    @After
    fun baseTearDown() {
        onTearDown()
        stopKoin()
        Dispatchers.resetMain()
    }
}
