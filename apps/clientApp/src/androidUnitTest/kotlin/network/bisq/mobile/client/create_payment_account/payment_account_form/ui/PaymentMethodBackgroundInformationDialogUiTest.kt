package network.bisq.mobile.client.create_payment_account.payment_account_form.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class PaymentMethodBackgroundInformationDialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var externalUrlOpener: CapturingExternalUrlOpener

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        externalUrlOpener = CapturingExternalUrlOpener()
    }

    private fun setTestContent(
        bodyText: String,
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides externalUrlOpener) {
                BisqTheme {
                    PaymentMethodBackgroundInformationDialog(
                        bodyText = bodyText,
                        onDismissRequest = onDismissRequest,
                    )
                }
            }
        }
    }

    @Test
    fun `renders dialog and clicking i understand dismisses`() {
        var dismissCount = 0
        setTestContent(
            bodyText = "Zelle info body",
            onDismissRequest = { dismissCount++ },
        )

        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.accountData.backgroundOverlay.headline".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Zelle info body").assertIsDisplayed()

        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun `when body contains hyperlink token then url is rendered`() {
        setTestContent(
            bodyText = "Read more at [HYPERLINK:https://example.com] now.",
        )

        composeTestRule.onNodeWithText("https://example.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun `blank hyperlink token keeps raw token and does not open uri`() {
        val text = "Read more at [HYPERLINK:   ] now."
        setTestContent(bodyText = text)

        composeTestRule.onNodeWithText(text, substring = true).assertIsDisplayed()
        assertEquals(emptyList(), externalUrlOpener.openedUrls)
    }

    @Test
    fun `when hyperlink clicked then opens url via external url opener`() {
        setTestContent(bodyText = "Read more at [HYPERLINK:https://example.com/path] now.")

        composeTestRule.onNodeWithText("https://example.com/path", substring = true).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("https://example.com/path"), externalUrlOpener.openedUrls)
    }

    private class CapturingExternalUrlOpener : ExternalUrlOpener {
        val openedUrls = mutableListOf<String>()

        override suspend fun openUrl(url: String): Boolean {
            openedUrls += url
            return true
        }
    }
}
