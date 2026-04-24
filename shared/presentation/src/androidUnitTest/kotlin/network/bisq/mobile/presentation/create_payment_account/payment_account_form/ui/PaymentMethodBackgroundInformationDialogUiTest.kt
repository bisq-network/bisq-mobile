package network.bisq.mobile.presentation.create_payment_account.payment_account_form.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.ui.PaymentMethodBackgroundInformationDialog
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PaymentMethodBackgroundInformationDialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var uriHandler: CapturingUriHandler

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        uriHandler = CapturingUriHandler()
    }

    private fun setTestContent(
        bodyText: String,
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
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
        assertEquals(emptyList(), uriHandler.openedUris)
    }

    private class CapturingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }
}
