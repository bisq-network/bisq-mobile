package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(application = TestApplication::class)
class PaymentMethodBackgroundInformationDialogUiTest : BisqComposeUiTestBase() {
    private lateinit var externalUrlOpener: CapturingExternalUrlOpener

    override fun setUpUiTest() {
        super.setUpUiTest()
        externalUrlOpener = CapturingExternalUrlOpener()
    }

    private fun setDialogContent(
        bodyText: String,
        onDismissRequest: () -> Unit = {},
    ) {
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides externalUrlOpener) {
                PaymentMethodBackgroundInformationDialog(
                    bodyText = bodyText,
                    onDismissRequest = onDismissRequest,
                )
            }
        }
    }

    @Test
    fun `renders dialog and clicking i understand dismisses`() {
        var dismissCount = 0
        setDialogContent(
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
        setDialogContent(
            bodyText = "Read more at [HYPERLINK:https://example.com] now.",
        )

        composeTestRule.onNodeWithText("https://example.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun `blank hyperlink token keeps raw token and does not open uri`() {
        val text = "Read more at [HYPERLINK:   ] now."
        setDialogContent(bodyText = text)

        composeTestRule.onNodeWithText(text, substring = true).assertIsDisplayed()
        assertEquals(emptyList(), externalUrlOpener.openedUrls)
    }

    @Test
    fun `when hyperlink clicked then opens url via external url opener`() {
        setDialogContent(bodyText = "Read more at [HYPERLINK:https://example.com/path] now.")

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
