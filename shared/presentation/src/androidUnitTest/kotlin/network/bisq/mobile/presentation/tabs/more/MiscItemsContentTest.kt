package network.bisq.mobile.presentation.tabs.more

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.nav_ignored_users
import bisqapps.shared.presentation.generated.resources.nav_settings
import bisqapps.shared.presentation.generated.resources.nav_user
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class MiscItemsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        uiState: MiscItemsUiState,
        onAction: (MiscItemsUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    MiscItemsContent(uiState = uiState, onAction = onAction)
                }
            }
        }
    }

    @Test
    fun `when rendered then shows section headers in uppercase`() {
        setTestContent(uiState = sampleUiState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.more.section.identity".i18n().uppercase())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.more.section.app".i18n().uppercase())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when rendered then shows item labels`() {
        setTestContent(uiState = sampleUiState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.more.userProfile".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when enabled item clicked then dispatches OnMenuItemClick with its route`() {
        var capturedAction: MiscItemsUiAction? = null
        setTestContent(uiState = sampleUiState(), onAction = { capturedAction = it })
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.more.userProfile".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(MiscItemsUiAction.OnMenuItemClick(NavRoute.UserProfile), capturedAction)
    }

    @Test
    fun `when item is disabled then it is not enabled`() {
        setTestContent(uiState = sampleUiState(ignoredEnabled = false))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers".i18n())
            .assertIsNotEnabled()
    }

    @Test
    fun `when disabled item tapped then no action is dispatched`() {
        var capturedAction: MiscItemsUiAction? = null
        setTestContent(uiState = sampleUiState(ignoredEnabled = false), onAction = { capturedAction = it })
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertNull(capturedAction)
    }

    private fun sampleUiState(ignoredEnabled: Boolean = false) =
        MiscItemsUiState(
            sections =
                listOf(
                    MenuSection(
                        title = UiString("mobile.more.section.identity"),
                        items =
                            listOf(
                                MenuItem(
                                    label = UiString("mobile.more.userProfile"),
                                    icon = Res.drawable.nav_user,
                                    route = NavRoute.UserProfile,
                                ),
                                MenuItem(
                                    label = UiString("mobile.settings.ignoredUsers"),
                                    icon = Res.drawable.nav_ignored_users,
                                    route = NavRoute.IgnoredUsers,
                                    isEnabled = ignoredEnabled,
                                ),
                            ),
                    ),
                    MenuSection(
                        title = UiString("mobile.more.section.app"),
                        items =
                            listOf(
                                MenuItem(
                                    label = UiString("mobile.more.settings"),
                                    icon = Res.drawable.nav_settings,
                                    route = NavRoute.Settings,
                                ),
                            ),
                    ),
                ),
        )
}
