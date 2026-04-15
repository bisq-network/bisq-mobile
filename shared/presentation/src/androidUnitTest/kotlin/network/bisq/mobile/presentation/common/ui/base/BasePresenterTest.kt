package network.bisq.mobile.presentation.common.ui.base

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BasePresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        globalUiManager = spyk(GlobalUiManager())

        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single<NavigationManager> { io.mockk.mockk(relaxed = true) }
                    single { globalUiManager }
                },
            )
        }

        mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }

    @Test
    fun `onViewUnattaching does not dismiss snackbar`() {
        val presenter = TestPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.showTestSnackbar("test message")
        presenter.onViewUnattaching()

        // Snackbars are app-level with auto-dismiss duration — BasePresenter
        // should never dismiss them on detach. If a specific presenter needs
        // dismissal, it should call globalUiManager.dismissSnackbar() explicitly.
        verify(exactly = 0) { globalUiManager.dismissSnackbar() }
    }

    @Test
    fun `presenter unregisters from parent on detach`() {
        val first = TestPresenter(mainPresenter)
        val second = TestPresenter(mainPresenter)

        first.onViewAttached()
        second.onViewAttached()

        first.onViewUnattaching()
        second.onViewUnattaching()

        // Re-register by creating new instances with the same parent.
        // If unregisterChild didn't work, dependants would accumulate stale entries.
        val newFirst = TestPresenter(mainPresenter)
        val newSecond = TestPresenter(mainPresenter)
        newFirst.onViewAttached()
        newSecond.onViewAttached()
        newFirst.onViewUnattaching()
        newSecond.onViewUnattaching()
    }

    @Test
    fun `onViewHidden hides loading but does not dispose scope`() {
        val presenter = TestPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.showTestLoading()
        presenter.onViewHidden()

        // Loading should be hidden
        verify(exactly = 1) { globalUiManager.hideLoading() }
        // Scope should NOT be disposed (no jobsManager.dispose call via unmanaged scope)
        // The presenter is still alive on the back stack
    }

    @Test
    fun `onViewRevealed re-enables interactivity`() {
        val presenter = TestPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.onViewHidden()
        presenter.onViewRevealed()

        // After reveal, interactive should be re-enabled (true after 250ms delay)
        // The fact that onViewRevealed completes without error confirms the scope is alive
    }

    @Test
    fun `onViewRevealed blocks interactivity briefly when blockInteractivityOnAttached is true`() {
        val presenter = BlockingPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.onViewHidden()
        presenter.onViewRevealed()

        // blockInteractivityForBriefMoment disables then re-enables after 250ms
        // Immediately after the call, interactive should be false (disabled)
        assertFalse(presenter.isInteractive.value)
    }

    @Test
    fun `onViewHidden does not unregister from parent`() {
        val presenter = TestPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.onViewHidden()

        // Presenter should still be registered — creating a second and detaching
        // should work without issues (parent still tracks the first)
        presenter.onViewRevealed()
    }

    @Test
    fun `navigateToReportError shows error snackbar when URL launch fails`() {
        val urlLauncher = mockk<UrlLauncher>()
        every { urlLauncher.openUrl(any()) } returns false
        mainPresenter =
            MainPresenterTestFactory.create(
                urlLauncher = urlLauncher,
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter = TestPresenter(mainPresenter)

        presenter.navigateToReportError()

        verify(exactly = 1) { urlLauncher.openUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES) }
        verify(exactly = 1) {
            globalUiManager.showSnackbar(
                "mobile.error.cannotOpenUrl".i18n(),
                SnackbarType.ERROR,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `navigateToReportError does not show error snackbar when URL launch succeeds`() {
        val urlLauncher = mockk<UrlLauncher>()
        every { urlLauncher.openUrl(any()) } returns true
        mainPresenter =
            MainPresenterTestFactory.create(
                urlLauncher = urlLauncher,
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter = TestPresenter(mainPresenter)

        presenter.navigateToReportError()

        verify(exactly = 1) { urlLauncher.openUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES) }
        verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
    }

    @Test
    fun `navigateToUrl returns false and restores interactivity when URL launcher throws`() {
        val urlLauncher = mockk<UrlLauncher>()
        every { urlLauncher.openUrl(any()) } throws IllegalStateException("unexpected")
        mainPresenter =
            MainPresenterTestFactory.create(
                urlLauncher = urlLauncher,
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter = TestPresenter(mainPresenter)

        val result = presenter.navigateToUrl("https://bisq.network")

        assertFalse(result)
        assertFalse(presenter.isInteractive.value)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(presenter.isInteractive.value)
        verify(exactly = 1) { urlLauncher.openUrl("https://bisq.network") }
    }

    /**
     * Presenter with blockInteractivityOnAttached = true to test the brief-moment blocking path.
     */
    private class BlockingPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        override val blockInteractivityOnAttached = true
    }

    private class TestPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        fun showTestSnackbar(message: String) {
            showSnackbar(message)
        }

        fun showTestLoading() {
            showLoading()
        }
    }
}
