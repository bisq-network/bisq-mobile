package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.ZelleFormUiAction
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ZelleFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        uiState: ZelleFormUiState = sampleUiState(),
        onAction: (ZelleFormUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    ZelleFormContent(
                        uiState = uiState,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    @Test
    fun `renders zelle form fields and background dialog by default`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.accountData.backgroundOverlay.headline".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when dismissing background dialog then it is hidden`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("action.iUnderstand".i18n())
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when holder name field typed then emits holder name change action`() {
        val holderName = "Alice Doe"
        var capturedAction: ZelleFormUiAction? = null
        setTestContent(onAction = { action -> capturedAction = action })
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.holderName".i18n().lowercase(),
                ),
            ).performTextInput(holderName)

        composeTestRule.waitForIdle()
        assertEquals(ZelleFormUiAction.OnHolderNameChange(holderName), capturedAction)
    }

    @Test
    fun `when email mobile field typed then emits email mobile change action`() {
        val emailOrMobile = "alice@example.com"
        var capturedAction: ZelleFormUiAction? = null
        setTestContent(onAction = { action -> capturedAction = action })
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.emailOrMobileNr".i18n().lowercase(),
                ),
            ).performTextInput(emailOrMobile)

        composeTestRule.waitForIdle()
        assertEquals(ZelleFormUiAction.OnEmailOrMobileNrChange(emailOrMobile), capturedAction)
    }

    private fun sampleUiState(): ZelleFormUiState =
        ZelleFormUiState(
            holderNameEntry = DataEntry(value = ""),
            emailOrMobileNrEntry = DataEntry(value = ""),
        )
}
