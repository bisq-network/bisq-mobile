package network.bisq.mobile.client.splash

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.startup.splash.SplashActiveDialog
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.startup.splash.SplashUiAction
import network.bisq.mobile.presentation.startup.splash.SplashUiState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class ClientSplashScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var presenter: SplashPresenter
    private lateinit var uiState: MutableStateFlow<SplashUiState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()

        uiState = MutableStateFlow(SplashUiState())
        presenter = mockk(relaxed = true)
        every { presenter.uiState } returns uiState

        // TestApplication already started Koin with clientTestModule; restart it so the screen's
        // koinInject<SplashPresenter>() resolves to our mock while keeping the standard deps.
        runCatching { stopKoin() }
        startKoin {
            modules(
                module { single<SplashPresenter> { presenter } },
                clientTestModule,
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `when uiState has app name and status then renders both`() {
        uiState.value =
            SplashUiState(
                appNameAndVersion = APP_NAME_AND_VERSION,
                status = STATUS,
            )

        setContent()

        composeTestRule.onNodeWithText(APP_NAME_AND_VERSION).assertIsDisplayed()
        composeTestRule.onNodeWithText(STATUS).assertIsDisplayed()
    }

    @Test
    fun `when active dialog is set then splash dialog is shown`() {
        uiState.value = SplashUiState(activeDialog = SplashActiveDialog.TorBootstrapFailed)

        setContent()

        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.title".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when no active dialog then no splash dialog is shown`() {
        uiState.value = SplashUiState(activeDialog = null)

        setContent()

        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.title".i18n()).assertDoesNotExist()
    }

    @Test
    fun `when dialog action clicked then forwards action to presenter`() {
        uiState.value = SplashUiState(activeDialog = SplashActiveDialog.TorBootstrapFailed)
        setContent()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()

        verify(exactly = 1) { presenter.onAction(SplashUiAction.OnPurgeRestartTor) }
    }

    @Test
    fun `when screen attached then applies route and attaches presenter`() {
        val route = NavRoute.Splash()

        setContent(route)

        verify(exactly = 1) { presenter.applyRoute(route) }
        verify(exactly = 1) { presenter.onViewAttached() }
    }

    private fun setContent(route: NavRoute.Splash = NavRoute.Splash()) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    ClientSplashScreen(route)
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val APP_NAME_AND_VERSION = "Bisq Connect 2.1.0"
        const val STATUS = "Bootstrap to P2P network"
    }
}
