package network.bisq.mobile.presentation.common.ui.base

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
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
    fun `onViewUnattaching does not dismiss snackbar by default`() {
        val presenter = TestPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.showTestSnackbar("test message")
        presenter.onViewUnattaching()

        assertFalse(presenter.dismissSnackbarOnDetachValue)
        verify(exactly = 0) { globalUiManager.dismissSnackbar() }
    }

    @Test
    fun `onViewUnattaching dismisses snackbar when dismissSnackbarOnDetach is true`() {
        val presenter = ContextualSnackbarPresenter(mainPresenter)

        presenter.onViewAttached()
        presenter.showTestSnackbar("screen-specific message")
        presenter.onViewUnattaching()

        assertTrue(presenter.dismissSnackbarOnDetachValue)
        verify(exactly = 1) { globalUiManager.dismissSnackbar() }
    }

    @Test
    fun `presenter unregisters from parent on detach regardless of dismissSnackbarOnDetach flag`() {
        val defaultPresenter = TestPresenter(mainPresenter)
        val contextualPresenter = ContextualSnackbarPresenter(mainPresenter)

        defaultPresenter.onViewAttached()
        contextualPresenter.onViewAttached()

        // Both are registered — calling onViewUnattaching twice on the same presenter
        // would crash if unregisterChild wasn't called (removing a non-existent child)
        defaultPresenter.onViewUnattaching()
        contextualPresenter.onViewUnattaching()

        // Re-register both by creating new instances with the same parent.
        // If unregisterChild didn't work, registerChild would accumulate stale entries.
        val newDefault = TestPresenter(mainPresenter)
        val newContextual = ContextualSnackbarPresenter(mainPresenter)
        newDefault.onViewAttached()
        newContextual.onViewAttached()
        newDefault.onViewUnattaching()
        newContextual.onViewUnattaching()
    }

    /**
     * Standard presenter with default behavior (dismissSnackbarOnDetach = false).
     * Snackbars survive navigation — they auto-dismiss via SnackbarDuration.
     */
    private class TestPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        val dismissSnackbarOnDetachValue get() = dismissSnackbarOnDetach

        fun showTestSnackbar(message: String) {
            showSnackbar(message)
        }
    }

    /**
     * Presenter that opts in to snackbar dismissal on detach.
     * For screens with contextual snackbars that should not survive navigation.
     */
    private class ContextualSnackbarPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        override val dismissSnackbarOnDetach = true

        val dismissSnackbarOnDetachValue get() = dismissSnackbarOnDetach

        fun showTestSnackbar(message: String) {
            showSnackbar(message)
        }
    }
}
