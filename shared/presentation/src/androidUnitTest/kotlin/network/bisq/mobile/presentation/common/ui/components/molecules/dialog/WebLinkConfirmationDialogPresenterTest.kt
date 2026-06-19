package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
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
class WebLinkConfirmationDialogPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settings: WebLinkDialogSettingsServiceFake
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: WebLinkConfirmationDialogPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.initialize("en")
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
        settings = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        mainPresenter = mockk(relaxed = true)
        presenter = WebLinkConfirmationDialogPresenter(settings, mainPresenter)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `open uri failure invokes error callback and re-enables open guard`() =
        runTest(testDispatcher) {
            var errorCalled = false
            initializePresenter(onError = { errorCalled = true })
            coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } throws RuntimeException("browser failed")

            presenter.onAction(WebLinkConfirmationUiAction.OnConfirm)
            advanceUntilIdle()

            assertTrue(errorCalled)
            assertTrue(presenter.isOpenUriEnabled.value)
            coVerify {
                mainPresenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
            }
        }

    @Test
    fun `open uri cancellation re-enables open guard`() =
        runTest(testDispatcher) {
            initializePresenter()
            coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } throws CancellationException("cancelled")

            try {
                presenter.onAction(WebLinkConfirmationUiAction.OnConfirm)
                advanceUntilIdle()
            } catch (_: CancellationException) {
            }

            assertTrue(presenter.isOpenUriEnabled.value)
        }

    @Test
    fun `copy to clipboard cancellation re-enables copy guard`() =
        runTest(testDispatcher) {
            val clipboard =
                mockk<Clipboard> {
                    coEvery { setClipEntry(any()) } throws CancellationException("cancelled")
                }
            initializePresenter(clipboard = clipboard)

            presenter.onAction(WebLinkConfirmationUiAction.OnDismiss(toCopy = true))
            try {
                advanceUntilIdle()
            } catch (_: CancellationException) {
            }

            assertTrue(presenter.isCopyToClipboardEnabled.value)
        }

    @Test
    fun `open uri returning false invokes error callback and re-enables guard`() =
        runTest(testDispatcher) {
            var errorCalled = false
            initializePresenter(onError = { errorCalled = true })
            coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns false

            presenter.onAction(WebLinkConfirmationUiAction.OnConfirm)
            advanceUntilIdle()

            assertTrue(errorCalled)
            assertTrue(presenter.isOpenUriEnabled.value)
        }

    private fun initializePresenter(
        clipboard: Clipboard = mockk(relaxed = true),
        onError: () -> Unit = {},
    ) {
        presenter.initialize(
            link = "https://example.com/test",
            clipboard = clipboard,
            onConfirm = {},
            onDismiss = {},
            onError = onError,
        )
    }
}
