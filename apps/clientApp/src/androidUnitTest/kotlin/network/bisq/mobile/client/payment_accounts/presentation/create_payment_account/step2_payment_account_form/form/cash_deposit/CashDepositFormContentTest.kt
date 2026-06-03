package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CreateCashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CashDepositFormContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        paymentAccountsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        runCatching { stopKoin() }
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        runCatching {
            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
        runCatching { stopKoin() }
        Dispatchers.resetMain()
    }

    private fun setTestContent(
        presenter: CashDepositFormPresenter = CashDepositFormPresenter(paymentAccountsServiceFacade, mainPresenter),
        paymentMethod: FiatPaymentMethod = samplePaymentMethod(),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalIsTest provides true,
                LocalExternalUrlOpener provides ExternalUrlOpener { false },
            ) {
                BisqTheme {
                    CashDepositFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                        paymentMethod = paymentMethod,
                    )
                }
            }
        }
        dismissBackgroundDialogIfPresent()
    }

    private fun dismissBackgroundDialogIfPresent() {
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("action.iUnderstand".i18n())
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.let {
                composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()
                composeTestRule.waitForIdle()
            }
    }

    @Test
    fun `when rendered then country dropdown prompt is shown before country selection`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("paymentAccounts.currency".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when country dropdown search typed then countries are filtered`() {
        setTestContent()

        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("mobile.components.dropdown.searchPlaceholder".i18n()).performTextInput("ger")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Germany").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("United States").assertCountEquals(0)
    }

    @Test
    fun `when selected country details load then dynamic fields are shown`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setTestContent()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("United States").performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

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
        composeTestRule.onAllNodesWithText("paymentAccounts.cashDeposit.requirements".i18n()).assertCountEquals(1)
    }

    @Test
    fun `when country details fail then error state is shown`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.failure(RuntimeException("boom"))

        setTestContent()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("United States").performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.error.title".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("paymentAccounts.currency".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when currency dropdown search typed then currencies are filtered`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setTestContent()
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

        setTestContent()
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
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.cashDeposit.requirements".i18n().lowercase()))
            .performTextInput("Bring receipt")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alice Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID-123").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bisq Bank").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("BANKUS33").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("001").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("123456789").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("NAT-123").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Bring receipt").assertCountEquals(1)
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())
            val presenter = CashDepositFormPresenter(paymentAccountsServiceFacade, mainPresenter)
            var navigatedAccount: CreatePaymentAccount? = null

            setTestContent(
                presenter = presenter,
                onNavigateToNextScreen = { account -> navigatedAccount = account },
            )
            composeTestRule.waitForIdle()
            presenter.onAction(CashDepositFormUiAction.OnCountrySelect(2))
            advanceUntilIdle()
            presenter.onAction(CashDepositFormUiAction.OnCurrencySelect(1))
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Cash Deposit Main"))
            presenter.onAction(CashDepositFormUiAction.OnHolderNameChange(" Alice Doe "))
            presenter.onAction(CashDepositFormUiAction.OnHolderIdChange(" ID-123 "))
            presenter.onAction(CashDepositFormUiAction.OnBankNameChange(" Bisq Bank "))
            presenter.onAction(CashDepositFormUiAction.OnBankIdChange(" BANKUS33 "))
            presenter.onAction(CashDepositFormUiAction.OnBranchIdChange(" 001 "))
            presenter.onAction(CashDepositFormUiAction.OnAccountNrChange(" 123456789 "))
            presenter.onAction(CashDepositFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))
            presenter.onAction(CashDepositFormUiAction.OnNationalAccountIdChange(" NAT-123 "))
            presenter.onAction(CashDepositFormUiAction.OnRequirementsChange(" Bring receipt "))
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            val account = assertNotNull(navigatedAccount) as CreateCashDepositAccount
            assertEquals("Cash Deposit Main", account.accountName)
            assertEquals("US", account.accountPayload.selectedCountryCode)
            assertEquals("GBP", account.accountPayload.selectedCurrencyCode)
            assertEquals("Alice Doe", account.accountPayload.holderName)
            assertEquals("ID-123", account.accountPayload.holderId)
            assertEquals("Bisq Bank", account.accountPayload.bankName)
            assertEquals("BANKUS33", account.accountPayload.bankId)
            assertEquals("001", account.accountPayload.branchId)
            assertEquals("123456789", account.accountPayload.accountNr)
            assertEquals(BankAccountType.CHECKING, account.accountPayload.bankAccountType)
            assertEquals("NAT-123", account.accountPayload.nationalAccountId)
            assertEquals("Bring receipt", account.accountPayload.requirements)
        }

    private fun selectUnitedStates() {
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.accountData.country.prompt".i18n()).performClick()
        composeTestRule.onNodeWithText("United States").performClick()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.CASH_DEPOSIT,
            name = "Cash Deposit",
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

    private fun sampleCountryDetails(): BankAccountCountryDetails =
        BankAccountCountryDetails(
            country = Country("US", "United States"),
            bankAccountValidationSupported = true,
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
}
