package network.bisq.mobile.client.trusted_node_setup.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class SubscriptionsFailedDialogFactoryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setTestContent(onAction: (SubscriptionsFailedDialogUiAction) -> Unit = {}) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    SubscriptionsFailedDialog(
                        state =
                            SubscriptionsFailedDialogUiState(
                                listOf(
                                    Topic.MARKET_PRICE,
                                    Topic.TRADE_PROPERTIES,
                                    Topic.NUM_OFFERS,
                                ),
                            ),
                        onAction = onAction,
                    )
                }
            }
        }
    }

    private fun assertTextPresent(text: String) {
        composeTestRule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
    }

    @Test
    fun `dialog renders grouped severities and stays visible on back press`() {
        val receivedActions = mutableListOf<SubscriptionsFailedDialogUiAction>()

        setTestContent(onAction = receivedActions::add)
        composeTestRule.waitForIdle()

        assertTextPresent("mobile.client.dialog.failed_subs.title".i18n())
        assertTextPresent("mobile.client.dialog.failed_subs.core.title".i18n().uppercase())
        assertTextPresent("mobile.client.dialog.failed_subs.minor.title".i18n().uppercase())
        assertTextPresent(Topic.MARKET_PRICE.titleKey.i18n())
        assertTextPresent(Topic.TRADE_PROPERTIES.titleKey.i18n())
        assertTextPresent(Topic.NUM_OFFERS.titleKey.i18n())
        assertTextPresent(Topic.NUM_OFFERS.descriptionKey.i18n())

        pressBack()
        composeTestRule.waitForIdle()

        assertTextPresent("mobile.client.dialog.failed_subs.title".i18n())
        assertEquals(emptyList(), receivedActions)
    }

    @Test
    fun `dialog retry button invokes retry callback`() {
        val receivedActions = mutableListOf<SubscriptionsFailedDialogUiAction>()

        setTestContent(onAction = receivedActions::add)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasText("mobile.action.retry".i18n()) and hasClickAction())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf<SubscriptionsFailedDialogUiAction>(SubscriptionsFailedDialogUiAction.OnRetryPress),
            receivedActions,
        )
    }

    @Test
    fun `dialog continue button invokes continue callback`() {
        val receivedActions = mutableListOf<SubscriptionsFailedDialogUiAction>()

        setTestContent(onAction = receivedActions::add)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasText("mobile.client.dialog.failed_subs.continue".i18n()) and hasClickAction())
            .performScrollTo()
        composeTestRule
            .onNode(hasText("mobile.client.dialog.failed_subs.continue".i18n()) and hasClickAction())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf<SubscriptionsFailedDialogUiAction>(SubscriptionsFailedDialogUiAction.OnContinuePress),
            receivedActions,
        )
    }
}
