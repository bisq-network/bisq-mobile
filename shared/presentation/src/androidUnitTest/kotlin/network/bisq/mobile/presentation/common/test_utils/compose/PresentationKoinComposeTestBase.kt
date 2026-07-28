package network.bisq.mobile.presentation.common.test_utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeTestSupport.setBisqTestContent
import network.bisq.mobile.presentation.common.test_utils.coroutines.PresentationKoinTestBase
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * Base for `:shared:presentation` Compose UI tests that need Koin and
 * [presentationTestModule]. Subclass [PlatformPresentationKoinComposeTestBase] when static
 * platform APIs must be mocked (e.g. [network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp]).
 *
 * Uses Compose UI Test v2 ([createComposeRule] with [StandardTestDispatcher]) so composition
 * effects share [testDispatcher] with Koin/Main. [setTestContent] pumps via
 * [composeTestRule.waitForIdle] after set; call [composeTestRule.waitForIdle] again after
 * interactions. Prefer that over [kotlinx.coroutines.test.advanceUntilIdle].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
abstract class PresentationKoinComposeTestBase : PresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule(effectContext = testDispatcher)

    override fun onSetup() {
        I18nSupport.setLanguage()
        stubDefaultNavigationManager()
        onKoinReady()
    }

    /** Relaxed [navigationManager] mocks need explicit tab/back stubs when composables render TopBar. */
    protected open fun stubDefaultNavigationManager() {
        every { navigationManager.currentTab } returns MutableStateFlow(null)
        every { navigationManager.showBackButton() } returns false
    }

    protected fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setBisqTestContent(content)
        composeTestRule.waitForIdle()
    }

    /**
     * Mid-test Koin reconfiguration. Prefer mutating fakes or [io.mockk.coEvery] on existing
     * mocks instead. Does not re-run [beforeStartKoin]; existing navigation/UI manager mocks
     * are reused.
     */
    protected fun restartKoinWith(vararg extraModules: Module) {
        stopKoin()
        startKoin {
            modules(baseModules())
            modules(additionalModules())
            modules(extraModules.toList())
        }
        onKoinReady()
    }
}
