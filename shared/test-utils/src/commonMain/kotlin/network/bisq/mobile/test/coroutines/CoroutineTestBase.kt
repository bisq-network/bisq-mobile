package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest as kotlinxRunTest

/**
 * Internal building block for shared test base classes (`PresentationKoinTestBase`,
 * `KoinIntegrationTestBase`, and app-specific subclasses such as `ClientKoinIntegrationTestBase`).
 *
 * Test authors should extend a leaf base that matches their layer — not this class directly.
 * In test bodies, call the inherited [runTest] so work runs on [testDispatcher] (Main is set in setup).
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class CoroutineTestBase {
    protected open val testDispatcher: TestDispatcher = StandardTestDispatcher()

    protected fun setUpCoroutines() {
        Dispatchers.setMain(testDispatcher)
    }

    protected fun tearDownCoroutines() {
        Dispatchers.resetMain()
    }

    /**
     * Runs [block] in a [TestScope] that shares [testDispatcher]'s virtual-time scheduler.
     * Requires [setUpCoroutines] to have been called first.
     */
    protected fun runTest(block: suspend TestScope.() -> Unit) = kotlinxRunTest(testBody = block)
}
