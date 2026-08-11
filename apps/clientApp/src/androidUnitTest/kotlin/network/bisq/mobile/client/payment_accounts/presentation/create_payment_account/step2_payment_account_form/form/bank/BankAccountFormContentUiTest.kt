package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.coEvery
import io.mockk.mockk
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(application = TestApplication::class)
class BankAccountFormContentUiTest : BisqComposeUiTestBase() {
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter

    override fun setUpUiTest() {
        super.setUpUiTest()
        paymentAccountsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
    }

    private fun setFormContent(
        presenter: TestBankAccountFormPresenter = TestBankAccountFormPresenter(paymentAccountsServiceFacade, mainPresenter),
        paymentMethod: FiatPaymentMethod = samplePaymentMethod(),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        setTestContent {
            BankAccountFormContent(
                presenter = presenter,
                onNavigateToNextScreen = onNavigateToNextScreen,
                paymentMethod = paymentMethod,
            )
        }
    }

    @Test
    fun `when rendered then country dropdown prompt is shown before country selection`() {
        setFormContent()

        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("paymentAccounts.currency".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Account number").assertCountEquals(0)
    }

    @Test
    fun `when country dropdown search typed then countries are filtered`() {
        setFormContent()

        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("mobile.components.dropdown.searchPlaceholder".i18n()).performTextInput("ger")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Germany").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("United States").assertCountEquals(0)
    }

    @Test
    fun `when selected country details load then dynamic fields are shown`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setFormContent()
        selectUnitedStates()

        composeTestRule.onNodeWithText("paymentAccounts.currency".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.currency.prompt".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Account owner ID").assertCountEquals(1)
        composeTestRule.onNodeWithText("paymentAccounts.bank.bankName".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Branch number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Account number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt".i18n()).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("National account number").assertCountEquals(1)
    }

    @Test
    fun `when country details fail then error state is shown`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.failure(RuntimeException("boom"))

        setFormContent()
        selectUnitedStates()

        composeTestRule.onNodeWithText("mobile.error.title".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("paymentAccounts.currency".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when currency dropdown search typed then currencies are filtered`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setFormContent()
        selectUnitedStates()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.currency.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("mobile.components.dropdown.searchPlaceholder".i18n()).performTextInput("eur")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("USD (US Dollar)").assertCountEquals(0)
    }

    @Test
    fun `when text fields typed then visible inputs update through presenter`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setFormContent()
        selectUnitedStates()

        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.holderName".i18n().lowercase()))
            .performTextInput("Alice Doe")
        composeTestRule.onNodeWithText("Account owner ID").performTextInput("ID-123")
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.bank.bankName".i18n().lowercase()))
            .performTextInput("Bisq Bank")
        composeTestRule.onNodeWithText("Routing number").performTextInput("BANKUS33")
        composeTestRule.onNodeWithText("Branch number").performTextInput("001")
        composeTestRule.onNodeWithText("Account number").performTextInput("123456789")
        composeTestRule.onNodeWithText("National account number").performTextInput("NAT-123")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alice Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID-123").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bisq Bank").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("BANKUS33").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("001").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("123456789").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("NAT-123").assertCountEquals(1)
    }

    @Test
    fun `when country does not support bank validation then only account number field is shown`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns
            Result.success(sampleCountryDetails(bankAccountValidationSupported = false))

        setFormContent()
        selectUnitedStates()

        composeTestRule.onNodeWithText("paymentAccounts.currency".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Account number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("paymentAccounts.holderName".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Account owner ID").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.bank.bankName".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Branch number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("National account number").assertCountEquals(0)
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())
        val presenter = TestBankAccountFormPresenter(paymentAccountsServiceFacade, mainPresenter)
        var navigatedAccount: CreatePaymentAccount? = null

        setFormContent(
            presenter = presenter,
            onNavigateToNextScreen = { account -> navigatedAccount = account },
        )
        presenter.onAction(BankAccountFormUiAction.OnCountrySelect(2))
        composeTestRule.waitForIdle()
        presenter.onAction(BankAccountFormUiAction.OnCurrencySelect(2))
        presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Bank Account Main"))
        presenter.onAction(BankAccountFormUiAction.OnHolderNameChange(" Alice Doe "))
        presenter.onAction(BankAccountFormUiAction.OnHolderIdChange(" ID-123 "))
        presenter.onAction(BankAccountFormUiAction.OnBankNameChange(" Bisq Bank "))
        presenter.onAction(BankAccountFormUiAction.OnBankIdChange(" BANKUS33 "))
        presenter.onAction(BankAccountFormUiAction.OnBranchIdChange(" 001 "))
        presenter.onAction(BankAccountFormUiAction.OnAccountNrChange(" 123456789 "))
        presenter.onAction(BankAccountFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))
        presenter.onAction(BankAccountFormUiAction.OnNationalAccountIdChange(" NAT-123 "))
        presenter.onCommonAction(AccountFormUiAction.OnNextClick)
        composeTestRule.waitForIdle()

        val account = assertNotNull(navigatedAccount) as TestCreateBankAccount
        assertEquals("Bank Account Main", account.accountName)
        assertEquals("US", account.accountPayload.selectedCountryCode)
        assertEquals("USD", account.accountPayload.selectedCurrencyCode)
        assertEquals("Alice Doe", account.accountPayload.holderName)
        assertEquals("ID-123", account.accountPayload.holderId)
        assertEquals("Bisq Bank", account.accountPayload.bankName)
        assertEquals("BANKUS33", account.accountPayload.bankId)
        assertEquals("001", account.accountPayload.branchId)
        assertEquals("123456789", account.accountPayload.accountNr)
        assertEquals(BankAccountType.CHECKING, account.accountPayload.bankAccountType)
        assertEquals("NAT-123", account.accountPayload.nationalAccountId)
    }

    private fun selectUnitedStates() {
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("United States").performClick()
        composeTestRule.waitForIdle()
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.SAME_BANK,
            name = "Same Bank",
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "British Pound"),
                ),
            supportedCountries =
                listOf(
                    Country(code = "US", name = "United States"),
                    Country(code = "DE", name = "Germany"),
                    Country(code = "GB", name = "United Kingdom"),
                ),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

    private fun sampleCountryDetails(bankAccountValidationSupported: Boolean = true): BankAccountCountryDetails =
        BankAccountCountryDetails(
            country = Country("US", "United States"),
            bankAccountValidationSupported = bankAccountValidationSupported,
            holderIdRequired = true,
            holderIdDescription = "Account owner ID",
            holderIdDescriptionShort = "Owner ID",
            bankAccountTypeRequired = true,
            bankNameRequired = true,
            bankIdRequired = true,
            bankIdDescription = "Routing number",
            bankIdDescriptionShort = "Routing",
            branchIdRequired = true,
            branchIdDescription = "Branch number",
            branchIdDescriptionShort = "Branch",
            accountNrDescription = "Account number",
            nationalAccountIdRequired = true,
            nationalAccountIdDescription = "National account number",
            nationalAccountIdDescriptionShort = "National ID",
        )

    private data class TestCreateBankAccount(
        override val accountName: String,
        override val accountPayload: TestCreateBankAccountPayload,
    ) : CreatePaymentAccount

    private data class TestCreateBankAccountPayload(
        val selectedCountryCode: String,
        val selectedCurrencyCode: String,
        val holderName: String? = null,
        val holderId: String? = null,
        val bankName: String? = null,
        val bankId: String? = null,
        val branchId: String? = null,
        val accountNr: String,
        val bankAccountType: BankAccountType? = null,
        val nationalAccountId: String? = null,
    ) : CreatePaymentAccountPayload

    private class TestBankAccountFormPresenter(
        paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
        mainPresenter: MainPresenter,
    ) : BankAccountFormPresenter<TestCreateBankAccount>(paymentAccountsServiceFacade, mainPresenter) {
        override fun createAccount(
            accountName: String,
            payloadData: BankAccountCreatePayloadData,
        ): TestCreateBankAccount =
            TestCreateBankAccount(
                accountName = accountName,
                accountPayload =
                    TestCreateBankAccountPayload(
                        selectedCountryCode = payloadData.selectedCountryCode,
                        selectedCurrencyCode = payloadData.selectedCurrencyCode,
                        holderName = payloadData.holderName,
                        holderId = payloadData.holderId,
                        bankName = payloadData.bankName,
                        bankId = payloadData.bankId,
                        branchId = payloadData.branchId,
                        accountNr = payloadData.accountNr,
                        bankAccountType = payloadData.bankAccountType,
                        nationalAccountId = payloadData.nationalAccountId,
                    ),
            )
    }
}
