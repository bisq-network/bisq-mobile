package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(application = TestApplication::class)
class WiseFormContentUiTest : BisqComposeUiTestBase() {
    private lateinit var mainPresenter: MainPresenter

    override fun setUpUiTest() {
        super.setUpUiTest()
        mainPresenter = mockk(relaxed = true)
    }

    private fun setFormContent(
        presenter: WiseFormPresenter = WiseFormPresenter(mainPresenter),
        paymentMethod: FiatPaymentMethod = samplePaymentMethod(),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        setTestContent {
            WiseFormContent(
                presenter = presenter,
                onNavigateToNextScreen = onNavigateToNextScreen,
                paymentMethod = paymentMethod,
            )
        }
    }

    @Test
    fun `when rendered then wise form fields and currency summary are shown`() {
        setFormContent()

        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.email".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3)).assertIsDisplayed()
    }

    @Test
    fun `when holder name field typed then visible input updates through presenter`() {
        val holderName = "Alice Doe"
        setFormContent()

        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.holderName".i18n().lowercase(),
                ),
            ).performTextInput(holderName)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(holderName).assertIsDisplayed()
    }

    @Test
    fun `when email field typed then visible input updates through presenter`() {
        val email = "alice@example.com"
        setFormContent()

        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.email".i18n().lowercase(),
                ),
            ).performTextInput(email)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(email).assertIsDisplayed()
    }

    @Test
    fun `when currency summary clicked then picker controls and currencies are shown`() {
        setFormContent()

        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.picker.selectAll".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.picker.clearAll".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.searchHint".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onNodeWithText("GBP (British Pound)").assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
    }

    @Test
    fun `when picker clear all clicked then currency summary updates through presenter`() {
        setFormContent()

        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.picker.clearAll".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("mobile.paymentAccounts.currencyPicker.summary".i18n(0, 3))
            .assertCountEquals(2)
    }

    @Test
    fun `when picker currency clicked then currency summary updates through presenter`() {
        setFormContent()

        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("GBP (British Pound)").performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("mobile.paymentAccounts.currencyPicker.summary".i18n(2, 3))
            .assertCountEquals(2)
    }

    @Test
    fun `when picker search typed then list filters through presenter`() {
        setFormContent()

        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.searchHint".i18n()).performTextInput("eur")

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("GBP (British Pound)").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("USD (US Dollar)").assertCountEquals(0)
    }

    @Test
    fun `when picker search has no results then empty state is shown`() {
        setFormContent()

        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.searchHint".i18n()).performTextInput("xyz")

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.noResults".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("EUR (Euro)").assertCountEquals(0)
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() {
        val presenter = WiseFormPresenter(mainPresenter)
        var navigatedAccount: CreatePaymentAccount? = null

        setFormContent(
            presenter = presenter,
            onNavigateToNextScreen = { account -> navigatedAccount = account },
        )

        presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Wise Personal"))
        presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
        presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
        presenter.onAction(WiseFormUiAction.OnCurrencyToggle("GBP"))
        presenter.onCommonAction(AccountFormUiAction.OnNextClick)
        composeTestRule.waitForIdle()

        val account = assertNotNull(navigatedAccount) as CreateWiseAccount
        assertEquals("Wise Personal", account.accountName)
        assertEquals("John Doe", account.accountPayload.holderName)
        assertEquals("john@example.com", account.accountPayload.email)
        assertEquals(listOf("EUR", "USD"), account.accountPayload.selectedCurrencies.map { currency -> currency.code })
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.WISE,
            name = "Wise",
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "British Pound"),
                ),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )
}
