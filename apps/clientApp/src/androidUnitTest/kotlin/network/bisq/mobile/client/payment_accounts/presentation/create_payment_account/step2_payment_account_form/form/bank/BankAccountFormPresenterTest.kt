package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BankAccountFormPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var presenter: TestBankAccountFormPresenter

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        paymentAccountsServiceFacade = mockk(relaxed = true)
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
        presenter = TestBankAccountFormPresenter(paymentAccountsServiceFacade, mockk<MainPresenter>(relaxed = true))
    }

    @After
    fun tearDown() {
        presenter.onDestroy()
        runCatching { stopKoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize sorts countries by name and currencies by code`() {
        presenter.initialize(samplePaymentMethod())

        assertEquals(
            listOf("FR", "DE", "US"),
            presenter.uiState.value.countries
                .map { country -> country.code },
        )
        assertEquals(
            listOf("EUR", "GBP", "USD"),
            presenter.uiState.value.currencies
                .map { currency -> currency.code },
        )
    }

    @Test
    fun `when country selected then resets dependent state and loads country details`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("DE") } returns Result.success(sampleCountryDetails(country = Country("DE", "Germany")))
            presenter.initialize(samplePaymentMethod())
            seedCountryDependentState()

            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(1))
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(1, state.selectedCountryIndex)
            assertEquals(Country("DE", "Germany"), state.selectedCountry)
            assertNull(state.countryErrorMessage)
            assertEquals(sampleCountryDetails(country = Country("DE", "Germany")), state.countryDetails)
            assertFalse(state.isLoadingCountryDetails)
            assertFalse(state.isCountryDetailsError)
            assertEquals("", state.holderNameEntry.value)
            assertEquals("", state.holderIdEntry.value)
            assertEquals("", state.bankNameEntry.value)
            assertEquals("", state.bankIdEntry.value)
            assertEquals("", state.branchIdEntry.value)
            assertNull(state.accountNrEntry.errorMessage)
            assertNull(state.selectedBankAccountType)
            assertNull(state.bankAccountTypeErrorMessage)
            assertEquals("", state.nationalAccountIdEntry.value)
        }

    @Test
    fun `when country details fail then error state is set`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.failure(RuntimeException("boom"))
            presenter.initialize(samplePaymentMethod())

            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(2))
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoadingCountryDetails)
            assertTrue(presenter.uiState.value.isCountryDetailsError)
            assertNull(presenter.uiState.value.countryDetails)
        }

    @Test
    fun `when invalid country index selected then state is unchanged`() =
        runTest(testDispatcher) {
            presenter.initialize(samplePaymentMethod())
            val initialState = presenter.uiState.value

            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(99))
            advanceUntilIdle()

            assertEquals(initialState, presenter.uiState.value)
        }

    @Test
    fun `when currency selected then updates selection and clears error`() =
        runTest(testDispatcher) {
            presenter.initialize(samplePaymentMethod())
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            assertNotNull(presenter.uiState.value.currencyErrorMessage)

            presenter.onAction(BankAccountFormUiAction.OnCurrencySelect(0))

            assertEquals(0, presenter.uiState.value.selectedCurrencyIndex)
            assertEquals(FiatCurrency("EUR", "Euro"), presenter.uiState.value.selectedCurrency)
            assertNull(presenter.uiState.value.currencyErrorMessage)
        }

    @Test
    fun `field actions update entries and bank account type selection clears error`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(2))
            advanceUntilIdle()
            presenter.onAction(BankAccountFormUiAction.OnHolderNameChange("Alice Doe"))
            presenter.onAction(BankAccountFormUiAction.OnHolderIdChange("ID-123"))
            presenter.onAction(BankAccountFormUiAction.OnBankNameChange("Bisq Bank"))
            presenter.onAction(BankAccountFormUiAction.OnBankIdChange("BANKUS33"))
            presenter.onAction(BankAccountFormUiAction.OnBranchIdChange("001"))
            presenter.onAction(BankAccountFormUiAction.OnAccountNrChange("123456789"))
            presenter.onAction(BankAccountFormUiAction.OnNationalAccountIdChange("NAT-123"))
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            assertNotNull(presenter.uiState.value.bankAccountTypeErrorMessage)

            presenter.onAction(BankAccountFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))

            val state = presenter.uiState.value
            assertEquals("Alice Doe", state.holderNameEntry.value)
            assertEquals("ID-123", state.holderIdEntry.value)
            assertEquals("Bisq Bank", state.bankNameEntry.value)
            assertEquals("BANKUS33", state.bankIdEntry.value)
            assertEquals("001", state.branchIdEntry.value)
            assertEquals("123456789", state.accountNrEntry.value)
            assertEquals("NAT-123", state.nationalAccountIdEntry.value)
            assertEquals(BankAccountType.CHECKING, state.selectedBankAccountType)
            assertNull(state.bankAccountTypeErrorMessage)
        }

    @Test
    fun `when next clicked with invalid fields then no effect and errors are set`() =
        runTest(testDispatcher) {
            presenter.initialize(samplePaymentMethod())
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(BankAccountFormUiAction.OnAccountNrChange(""))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertNotNull(presenter.uniqueAccountNameEntry.value.errorMessage)
            assertNotNull(presenter.uiState.value.countryErrorMessage)
            assertNotNull(presenter.uiState.value.currencyErrorMessage)
            assertNotNull(presenter.uiState.value.accountNrEntry.errorMessage)
        }

    @Test
    fun `when bank validation supported then required bank fields block next`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails(bankAccountValidationSupported = true))
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(2))
            advanceUntilIdle()
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Bank Account"))
            presenter.onAction(BankAccountFormUiAction.OnCurrencySelect(2))
            presenter.onAction(BankAccountFormUiAction.OnAccountNrChange("123456789"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertNotNull(presenter.uiState.value.holderNameEntry.errorMessage)
            assertNotNull(presenter.uiState.value.holderIdEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankNameEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankIdEntry.errorMessage)
            assertNotNull(presenter.uiState.value.branchIdEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankAccountTypeErrorMessage)
            assertNotNull(presenter.uiState.value.nationalAccountIdEntry.errorMessage)
        }

    @Test
    fun `when bank validation unsupported then bank specific fields do not block next`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails(bankAccountValidationSupported = false))
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(2))
            advanceUntilIdle()
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Bank Account"))
            presenter.onAction(BankAccountFormUiAction.OnCurrencySelect(2))
            presenter.onAction(BankAccountFormUiAction.OnAccountNrChange("123456789"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            val effect = effectDeferred.await()
            assertTrue(effect is BankAccountFormEffect.NavigateToNextScreen)
            assertNull(effect.account.accountPayload.holderName)
            assertNull(effect.account.accountPayload.holderId)
            assertNull(effect.account.accountPayload.bankName)
            assertNull(effect.account.accountPayload.bankId)
            assertNull(effect.account.accountPayload.branchId)
            assertNull(effect.account.accountPayload.bankAccountType)
            assertNull(effect.account.accountPayload.nationalAccountId)
        }

    @Test
    fun `when next clicked with valid fields then emits account with trimmed payload`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails(bankAccountValidationSupported = true))
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(BankAccountFormUiAction.OnCountrySelect(2))
            advanceUntilIdle()
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange(" Bank Account "))
            presenter.onAction(BankAccountFormUiAction.OnCurrencySelect(2))
            presenter.onAction(BankAccountFormUiAction.OnHolderNameChange(" Alice Doe "))
            presenter.onAction(BankAccountFormUiAction.OnHolderIdChange(" ID-123 "))
            presenter.onAction(BankAccountFormUiAction.OnBankNameChange(" Bisq Bank "))
            presenter.onAction(BankAccountFormUiAction.OnBankIdChange(" BANKUS33 "))
            presenter.onAction(BankAccountFormUiAction.OnBranchIdChange(" 001 "))
            presenter.onAction(BankAccountFormUiAction.OnAccountNrChange(" 123456789 "))
            presenter.onAction(BankAccountFormUiAction.OnBankAccountTypeSelect(BankAccountType.SAVINGS))
            presenter.onAction(BankAccountFormUiAction.OnNationalAccountIdChange(" NAT-123 "))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            val effect = effectDeferred.await()
            assertTrue(effect is BankAccountFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Bank Account", account.accountName)
            assertEquals("US", account.accountPayload.selectedCountryCode)
            assertEquals("USD", account.accountPayload.selectedCurrencyCode)
            assertEquals("Alice Doe", account.accountPayload.holderName)
            assertEquals("ID-123", account.accountPayload.holderId)
            assertEquals("Bisq Bank", account.accountPayload.bankName)
            assertEquals("BANKUS33", account.accountPayload.bankId)
            assertEquals("001", account.accountPayload.branchId)
            assertEquals("123456789", account.accountPayload.accountNr)
            assertEquals(BankAccountType.SAVINGS, account.accountPayload.bankAccountType)
            assertEquals("NAT-123", account.accountPayload.nationalAccountId)
        }

    @Test
    fun `validate holder name accepts trimmed valid value`() {
        assertNull(validateHolderName("  John Doe  "))
    }

    @Test
    fun `validate holder name rejects too short value`() {
        assertNotNull(validateHolderName("a"))
    }

    @Test
    fun `validate holder id accepts valid trimmed value`() {
        assertNull(validateHolderId("  AB  "))
    }

    @Test
    fun `validate holder id rejects too short value`() {
        assertNotNull(validateHolderId("a"))
    }

    @Test
    fun `validate bank name accepts valid trimmed value`() {
        assertNull(validateBankName("  Bisq Bank  "))
    }

    @Test
    fun `validate bank name rejects too short value`() {
        assertNotNull(validateBankName("a"))
    }

    @Test
    fun `validate bank id accepts valid trimmed value`() {
        assertNull(validateBankId("  BANKUS33  "))
    }

    @Test
    fun `validate bank id rejects too long value`() {
        assertNotNull(validateBankId("a".repeat(51)))
    }

    @Test
    fun `validate branch id accepts valid trimmed value`() {
        assertNull(validateBranchId("  001  "))
    }

    @Test
    fun `validate branch id rejects too long value`() {
        assertNotNull(validateBranchId("a".repeat(51)))
    }

    @Test
    fun `validate account number accepts valid trimmed value`() {
        assertNull(validateAccountNr("  123456789  "))
    }

    @Test
    fun `validate account number rejects empty value`() {
        assertNotNull(validateAccountNr("  "))
    }

    @Test
    fun `validate national account id accepts valid trimmed value`() {
        assertNull(validateNationalAccountId("  NAT-123  "))
    }

    @Test
    fun `validate national account id rejects too long value`() {
        assertNotNull(validateNationalAccountId("a".repeat(51)))
    }

    private fun seedCountryDependentState() {
        presenter.onAction(BankAccountFormUiAction.OnHolderNameChange("Alice Doe"))
        presenter.onAction(BankAccountFormUiAction.OnHolderIdChange("ID-123"))
        presenter.onAction(BankAccountFormUiAction.OnBankNameChange("Bisq Bank"))
        presenter.onAction(BankAccountFormUiAction.OnBankIdChange("BANKUS33"))
        presenter.onAction(BankAccountFormUiAction.OnBranchIdChange("001"))
        presenter.onAction(BankAccountFormUiAction.OnAccountNrChange(""))
        presenter.onAction(BankAccountFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))
        presenter.onAction(BankAccountFormUiAction.OnNationalAccountIdChange("NAT-123"))
        presenter.onCommonAction(AccountFormUiAction.OnNextClick)
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.SAME_BANK,
            name = "Same Bank",
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "Pound Sterling"),
                ),
            supportedCountries =
                listOf(
                    Country(code = "US", name = "United States"),
                    Country(code = "DE", name = "Germany"),
                    Country(code = "FR", name = "France"),
                ),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

    private fun sampleCountryDetails(
        country: Country = Country("US", "United States"),
        bankAccountValidationSupported: Boolean = true,
    ): BankAccountCountryDetails =
        BankAccountCountryDetails(
            country = country,
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
