package network.bisq.mobile.client.common.test_utils

import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.test.koin.KoinIntegrationTestBase
import org.koin.core.module.Module

/** Leaf base for client facades, services, and presenters ([clientTestModule] floor). */
abstract class ClientKoinIntegrationTestBase : KoinIntegrationTestBase() {
    override fun baseModules(): List<Module> = listOf(clientTestModule)
}
