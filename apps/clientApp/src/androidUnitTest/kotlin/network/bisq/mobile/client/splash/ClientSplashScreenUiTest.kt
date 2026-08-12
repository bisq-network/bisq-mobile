package network.bisq.mobile.client.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.client.common.test_utils.ClientInjectComposeUiTestBase
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.startup.splash.SplashActiveDialog
import network.bisq.mobile.presentation.startup.splash.SplashUiAction
import network.bisq.mobile.presentation.startup.splash.SplashUiState
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

class ClientSplashScreenUiTest : ClientInjectComposeUiTestBase() {
    private lateinit var presenter: ClientSplashPresenter
    private lateinit var clientUiState: MutableStateFlow<ClientSplashUiState>

    override fun onBeforeKoinStart() {
        clientUiState = MutableStateFlow(ClientSplashUiState())
        presenter = mockk(relaxed = true)
        every { presenter.clientUiState } returns clientUiState
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ClientSplashPresenter> { presenter }
            },
        )

    private fun setContent(route: NavRoute.Splash = NavRoute.Splash()) {
        setInjectTestContent {
            ClientSplashScreen(route)
        }
    }

    @Test
    fun `when uiState has app name and title then renders both`() {
        clientUiState.value =
            ClientSplashUiState(
                splashUiState = SplashUiState(appNameAndVersion = APP_NAME_AND_VERSION),
                title = UiString(CONNECT_TITLE_KEY),
            )

        setContent()

        composeTestRule.onNodeWithText(APP_NAME_AND_VERSION).assertIsDisplayed()
        composeTestRule.onNodeWithText(CONNECT_TITLE_KEY.i18n()).assertIsDisplayed()
    }

    @Test
    fun `when active dialog is set then splash dialog is shown`() {
        clientUiState.value =
            ClientSplashUiState(splashUiState = SplashUiState(activeDialog = SplashActiveDialog.TorBootstrapFailed))

        setContent()

        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.title".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when no active dialog then no splash dialog is shown`() {
        clientUiState.value = ClientSplashUiState(splashUiState = SplashUiState(activeDialog = null))

        setContent()

        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.title".i18n()).assertDoesNotExist()
    }

    @Test
    fun `when dialog action clicked then forwards action to presenter`() {
        clientUiState.value =
            ClientSplashUiState(splashUiState = SplashUiState(activeDialog = SplashActiveDialog.TorBootstrapFailed))
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

    private companion object {
        const val APP_NAME_AND_VERSION = "Bisq Connect 2.1.0"
        const val CONNECT_TITLE_KEY = "mobile.bootstrap.connect.title"
    }
}
