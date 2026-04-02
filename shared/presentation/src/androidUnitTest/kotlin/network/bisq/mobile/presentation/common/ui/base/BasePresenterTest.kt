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

    private class TestPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        fun showTestSnackbar(message: String) {
            showSnackbar(message)
        }
    }
}
