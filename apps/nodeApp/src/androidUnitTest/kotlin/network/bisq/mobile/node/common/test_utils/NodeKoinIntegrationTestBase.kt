package network.bisq.mobile.node.common.test_utils

import network.bisq.mobile.node.common.di.testModule
import network.bisq.mobile.test.koin.KoinIntegrationTestBase
import org.koin.core.module.Module

/** Leaf base for node presenters/facades ([testModule] floor). */
abstract class NodeKoinIntegrationTestBase : KoinIntegrationTestBase() {
    override fun baseModules(): List<Module> = listOf(testModule)
}
