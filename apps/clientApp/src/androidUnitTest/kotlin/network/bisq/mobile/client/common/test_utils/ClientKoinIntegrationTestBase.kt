package network.bisq.mobile.client.common.test_utils

import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.test.koin.KoinIntegrationTestBase
import org.koin.core.module.Module

/**
 * Leaf base for client-app tests that require the full [clientTestModule] Koin graph.
 *
 * Use for client facades, services, and presenters — not for `:shared:presentation`
 * presenter tests (use `PresentationKoinTestBase` there).
 *
 * ```kotlin
 * class MyIntegrationTest : ClientKoinIntegrationTestBase() {
 *     override fun additionalModules(): List<Module> = listOf(
 *         module { single<MyDependency> { mockk(relaxed = true) } }
 *     )
 *
 *     @Test
 *     fun `my test`() = runTest { /* ... */ }
 * }
 * ```
 */
abstract class ClientKoinIntegrationTestBase : KoinIntegrationTestBase() {
    override fun baseModules(): List<Module> = listOf(clientTestModule)
}
