package network.bisq.mobile.presentation.common.test_utils.coroutines

import io.mockk.mockk
import network.bisq.mobile.presentation.common.test_utils.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.test.koin.KoinIntegrationTestBase
import org.koin.core.module.Module

/**
 * Base for presenter unit tests in `:shared:presentation` that need Koin and a controlled
 * main dispatcher. Subclass [PlatformPresentationKoinTestBase] when static platform APIs
 * must be mocked (e.g. screen width).
 */
abstract class PresentationKoinTestBase : KoinIntegrationTestBase() {
    protected lateinit var navigationManager: NavigationManager
    protected lateinit var globalUiManager: GlobalUiManager

    protected open fun setUpPlatformMocks() {}

    protected open fun tearDownPlatformMocks() {}

    protected open fun onKoinReady() {}

    override fun baseModules(): List<Module> = listOf(presentationTestModule(testDispatcher, navigationManager, globalUiManager))

    override fun beforeStartKoin() {
        setUpPlatformMocks()
        navigationManager = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
    }

    override fun onSetup() {
        onKoinReady()
    }

    override fun onTearDown() {
        tearDownPlatformMocks()
    }
}
