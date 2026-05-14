package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.wise

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
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.WiseFormUiAction
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class WiseFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        uiState: WiseFormUiState = sampleUiState(),
        onAction: (AccountFormUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    WiseFormContent(
                        uiState = uiState,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    @Test
    fun `renders wise holder email and currency summary`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.email".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.currencies.summary".i18n(2, 3)).assertIsDisplayed()
    }

    @Test
    fun `when all currencies selected then renders all selected summary`() {
        setTestContent(uiState = sampleUiState(selectedCurrencyCodes = setOf("USD", "EUR", "GBP")))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.currencies.allSelected".i18n(3)).assertIsDisplayed()
    }

    @Test
    fun `when currency validation error exists then renders error message`() {
        val errorMessage = "mobile.paymentAccounts.wise.currencies.error".i18n()
        setTestContent(uiState = sampleUiState(currencyErrorMessage = errorMessage))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun `when holder name typed then emits action`() {
        val holderName = "Satoshi"
        var captured: WiseFormUiAction? = null
        setTestContent(onAction = { action -> captured = action as? WiseFormUiAction })

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.holderName".i18n().lowercase()))
            .performTextInput(holderName)

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnHolderNameChange(holderName), captured)
    }

    @Test
    fun `when email typed then emits action`() {
        val email = "satoshi@example.com"
        var captured: WiseFormUiAction? = null
        setTestContent(onAction = { action -> captured = action as? WiseFormUiAction })

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.email".i18n().lowercase()))
            .performTextInput(email)

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnEmailChange(email), captured)
    }

    @Test
    fun `when currency summary clicked then emits open picker action`() {
        var captured: WiseFormUiAction? = null
        setTestContent(onAction = { action -> captured = action as? WiseFormUiAction })

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.wise.currencies.summary".i18n(2, 3))
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnOpenCurrencyPicker, captured)
    }

    @Test
    fun `when picker is open then renders controls and currencies`() {
        setTestContent(uiState = sampleUiState(isCurrencyPickerOpen = true))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.selectAll".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.clearAll".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("mobile.paymentAccounts.wise.currencies.summary".i18n(2, 3)).assertCountEquals(2)
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.searchHint".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onNodeWithText("GBP (British Pound)").assertIsDisplayed()
    }

    @Test
    fun `when picker select all clicked then emits select all action`() {
        var captured: WiseFormUiAction? = null
        setTestContent(
            uiState = sampleUiState(isCurrencyPickerOpen = true),
            onAction = { action -> captured = action as? WiseFormUiAction },
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.selectAll".i18n()).performClick()

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnSelectAllCurrencies, captured)
    }

    @Test
    fun `when picker clear all clicked then emits clear all action`() {
        var captured: WiseFormUiAction? = null
        setTestContent(
            uiState = sampleUiState(isCurrencyPickerOpen = true),
            onAction = { action -> captured = action as? WiseFormUiAction },
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.clearAll".i18n()).performClick()

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnClearAllCurrencies, captured)
    }

    @Test
    fun `when picker currency clicked then emits toggle action`() {
        var captured: WiseFormUiAction? = null
        setTestContent(
            uiState = sampleUiState(isCurrencyPickerOpen = true),
            onAction = { action -> captured = action as? WiseFormUiAction },
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("GBP (British Pound)").performClick()

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnCurrencyToggle("GBP"), captured)
    }

    @Test
    fun `when picker search typed then emits search change action`() {
        var captured: WiseFormUiAction? = null
        setTestContent(
            uiState = sampleUiState(isCurrencyPickerOpen = true),
            onAction = { action -> captured = action as? WiseFormUiAction },
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.searchHint".i18n()).performTextInput("eur")

        composeTestRule.waitForIdle()
        assertEquals(WiseFormUiAction.OnCurrencySearchChange("eur"), captured)
    }

    @Test
    fun `when picker search matches one currency then filters list`() {
        setTestContent(uiState = sampleUiState(isCurrencyPickerOpen = true, currencySearchQuery = "eur"))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("USD (US Dollar)").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("GBP (British Pound)").assertCountEquals(0)
    }

    @Test
    fun `when picker search has no results then renders empty state`() {
        setTestContent(uiState = sampleUiState(isCurrencyPickerOpen = true, currencySearchQuery = "xyz"))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.noResults".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("USD (US Dollar)").assertCountEquals(0)
    }

    private fun sampleUiState(
        selectedCurrencyCodes: Set<String> = setOf("USD", "EUR"),
        currencyErrorMessage: String? = null,
        isCurrencyPickerOpen: Boolean = false,
        currencySearchQuery: String = "",
    ): WiseFormUiState =
        WiseFormUiState(
            holderNameEntry = DataEntry(value = ""),
            emailEntry = DataEntry(value = ""),
            availableCurrencies = sampleCurrencies(),
            selectedCurrencyCodes = selectedCurrencyCodes,
            currencyErrorMessage = currencyErrorMessage,
            isCurrencyPickerOpen = isCurrencyPickerOpen,
            currencySearchQuery = currencySearchQuery,
        )

    private fun sampleCurrencies(): List<WiseCurrencyItem> =
        listOf(
            WiseCurrencyItem("USD", "USD (US Dollar)"),
            WiseCurrencyItem("EUR", "EUR (Euro)"),
            WiseCurrencyItem("GBP", "GBP (British Pound)"),
        )
}
