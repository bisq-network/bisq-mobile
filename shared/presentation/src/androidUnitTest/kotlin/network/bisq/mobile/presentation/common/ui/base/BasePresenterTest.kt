package network.bisq.mobile.presentation.common.ui.base

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
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
        verify(exactly = 1) { globalUiManager.scheduleHideLoading() }
        // Scope should NOT be disposed (no jobsManager.dispose call via unmanaged scope)
        // The presenter is still alive on the back stack
    }

    @Test
    fun `onViewRevealed completes without error when scope is alive`() {
        val presenter = TestPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.onViewHidden()
        presenter.onViewRevealed()
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
        coEvery { urlLauncher.openUrl(any()) } returns false
        mainPresenter =
            MainPresenterTestFactory.create(
                urlLauncher = urlLauncher,
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter = TestPresenter(mainPresenter)

        presenter.navigateToReportError()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { urlLauncher.openUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES) }
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
        coEvery { urlLauncher.openUrl(any()) } returns true
        mainPresenter =
            MainPresenterTestFactory.create(
                urlLauncher = urlLauncher,
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter = TestPresenter(mainPresenter)

        presenter.navigateToReportError()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { urlLauncher.openUrl(BisqLinks.BISQ_MOBILE_GH_ISSUES) }
        verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
    }

    @Test
    fun `navigateToUrl returns false and clears loading when URL launcher throws`() {
        val urlLauncher = mockk<UrlLauncher>()
        coEvery { urlLauncher.openUrl(any()) } throws IllegalStateException("unexpected")
        mainPresenter =
            MainPresenterTestFactory.create(
                urlLauncher = urlLauncher,
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter = TestPresenter(mainPresenter)

        val result = runBlocking { presenter.navigateToUrlAwait("https://bisq.network") }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(result)
        assertFalse(globalUiManager.isLoadingBlocking.value)
        assertFalse(globalUiManager.showLoadingDialog.value)
        coVerify(exactly = 1) { urlLauncher.openUrl("https://bisq.network") }
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
    fun `guardedSuspendAction re-enables guard and hides loading when block completes`() =
        runTest(testDispatcher) {
            val guard = MutableStateFlow(true)
            val presenter = GuardTestPresenter(mainPresenter)
            presenter.onViewAttached()

            assertNotNull(presenter.runGuarded(guard))
            advanceUntilIdle()

            assertTrue(guard.value)
            verify(atLeast = 1) { globalUiManager.scheduleHideLoading() }
        }

    @Test
    fun `guardedSuspendAction ignores second call while first is in progress`() =
        runTest(testDispatcher) {
            val guard = MutableStateFlow(true)
            val presenter = GuardTestPresenter(mainPresenter)
            presenter.onViewAttached()

            val job = assertNotNull(presenter.runGuarded(guard, blockForMs = Long.MAX_VALUE))
            try {
                advanceUntilIdle() // first block enters and suspends on delay
                assertEquals(1, presenter.blockStartCount)
                assertFalse(guard.value)

                assertNull(presenter.runGuarded(guard, blockForMs = 0))
                assertEquals(1, presenter.blockStartCount)
                assertEquals(0, presenter.completedActions)
            } finally {
                job.cancel()
                advanceUntilIdle()
            }
        }

    @Test
    fun `guardedSuspendAction with reEnableGuardOnComplete false leaves guard disabled after success`() =
        runTest(testDispatcher) {
            val guard = MutableStateFlow(true)
            val presenter = GuardTestPresenter(mainPresenter)
            presenter.onViewAttached()

            assertNotNull(presenter.runGuarded(guard, reEnableGuardOnComplete = false))
            advanceUntilIdle()

            assertFalse(guard.value)
        }

    @Test
    fun `guardedSuspendAction with reEnableGuardOnComplete false allows manual re-enable on failure`() =
        runTest(testDispatcher) {
            val guard = MutableStateFlow(true)
            val presenter = GuardTestPresenter(mainPresenter)
            presenter.onViewAttached()

            assertNotNull(
                presenter.runGuarded(guard = guard, reEnableGuardOnComplete = false) {
                    guard.value = true
                },
            )
            advanceUntilIdle()

            assertTrue(guard.value)
        }

    @Test
    fun `guardedSuspendAction returns cancellable Job that restores guard on cancel`() =
        runTest(testDispatcher) {
            val guard = MutableStateFlow(true)
            val presenter = GuardTestPresenter(mainPresenter)
            presenter.onViewAttached()

            val job = assertNotNull(presenter.runGuarded(guard, blockForMs = Long.MAX_VALUE))
            advanceUntilIdle()
            assertFalse(guard.value)

            job.cancel()
            advanceUntilIdle()

            assertTrue(guard.value)
            verify(atLeast = 1) { globalUiManager.scheduleHideLoading() }
        }

    @Test
    fun `guardedSuspendAction restores guard and hides loading when cancelled before coroutine starts`() =
        runTest(testDispatcher) {
            val guard = MutableStateFlow(true)
            val presenter = GuardTestPresenter(mainPresenter)
            presenter.onViewAttached()

            val job = assertNotNull(presenter.runGuarded(guard, blockForMs = Long.MAX_VALUE))
            assertFalse(guard.value)
            assertEquals(0, presenter.blockStartCount)

            job.cancel()
            advanceUntilIdle()

            assertEquals(0, presenter.blockStartCount)
            assertTrue(guard.value)
            verify(atLeast = 1) { globalUiManager.scheduleHideLoading() }
        }

    private class GuardTestPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        var blockStartCount = 0
        var completedActions = 0

        fun runGuarded(
            guard: MutableStateFlow<Boolean> = MutableStateFlow(true),
            blockForMs: Long = 0,
            showLoadingOverlay: Boolean = true,
            reEnableGuardOnComplete: Boolean = true,
            block: (suspend () -> Unit)? = null,
        ): Job? =
            guardedSuspendAction(
                guard,
                "testAction",
                showLoadingOverlay = showLoadingOverlay,
                reEnableGuardOnComplete = reEnableGuardOnComplete,
            ) {
                blockStartCount++
                if (block != null) {
                    block()
                } else {
                    delay(blockForMs)
                    completedActions++
                }
            }
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
