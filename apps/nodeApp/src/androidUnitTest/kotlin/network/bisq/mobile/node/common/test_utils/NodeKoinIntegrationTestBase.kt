package network.bisq.mobile.node.common.test_utils

import network.bisq.mobile.node.common.di.testModule
import network.bisq.mobile.test.koin.KoinIntegrationTestBase
import org.koin.core.module.Module

/**
 * Leaf test base for node presenter tests. Provides the Koin floor (NavigationManager,
 * CoroutineJobsManager, GlobalUiManager) via [testModule] and the coroutine/dispatcher lifecycle
 * from [KoinIntegrationTestBase]. Mirrors `ClientKoinIntegrationTestBase`.
 */
abstract class NodeKoinIntegrationTestBase : KoinIntegrationTestBase() {
    override fun baseModules(): List<Module> = listOf(testModule)
}
