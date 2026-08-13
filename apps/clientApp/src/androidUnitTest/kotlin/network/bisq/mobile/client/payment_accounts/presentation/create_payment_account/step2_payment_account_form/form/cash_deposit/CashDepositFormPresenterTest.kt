package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CashDepositFormPresenterTest : ClientKoinIntegrationTestBase() {
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: CashDepositFormPresenter

    override fun onSetup() {
        presenter = CashDepositFormPresenter(paymentAccountsServiceFacade, mainPresenter)
    }

    override fun onTearDown() {
        try {
            if (::presenter.isInitialized) presenter.onDestroy()
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `when bank account validation supported then bank specific required fields block next`() =
        runTest {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns
                Result.success(sampleCountryDetails(bankAccountValidationSupported = true))
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(CashDepositFormUiAction.OnCountrySelect(0))
            advanceUntilIdle()
            fillCommonRequiredFields()

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertNotNull(presenter.uiState.value.bankNameEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankIdEntry.errorMessage)
            assertNotNull(presenter.uiState.value.branchIdEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankAccountTypeErrorMessage)
            assertNotNull(presenter.uiState.value.nationalAccountIdEntry.errorMessage)
        }

    @Test
    fun `when bank account validation unsupported then bank specific required fields do not block next`() =
        runTest {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns
                Result.success(sampleCountryDetails(bankAccountValidationSupported = false))
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(CashDepositFormUiAction.OnCountrySelect(0))
            advanceUntilIdle()
            fillCommonRequiredFields()

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            effectDeferred.await()
            assertNull(presenter.uiState.value.bankNameEntry.errorMessage)
            assertNull(presenter.uiState.value.bankIdEntry.errorMessage)
            assertNull(presenter.uiState.value.branchIdEntry.errorMessage)
            assertNull(presenter.uiState.value.bankAccountTypeErrorMessage)
            assertNull(presenter.uiState.value.nationalAccountIdEntry.errorMessage)
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
    fun `validate bank id rejects too long value`() {
        assertNotNull(validateBankId("a".repeat(51)))
    }

    @Test
    fun `validate branch id accepts one character value`() {
        assertNull(validateBranchId("1"))
    }

    @Test
    fun `validate account number rejects empty value`() {
        assertNotNull(validateAccountNr("  "))
    }

    @Test
    fun `validate account number rejects too long value`() {
        assertNotNull(validateAccountNr("1".repeat(51)))
    }

    @Test
    fun `validate national account id accepts one character value`() {
        assertNull(validateNationalAccountId("1"))
    }

    @Test
    fun `validate requirements accepts blank value`() {
        assertNull(validateCashDepositRequirements("  "))
    }

    @Test
    fun `validate requirements accepts max length value`() {
        assertNull(validateCashDepositRequirements("a".repeat(150)))
    }

    @Test
    fun `validate requirements rejects too long value`() {
        assertNotNull(validateCashDepositRequirements("a".repeat(151)))
    }

    private fun fillCommonRequiredFields() {
        presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Cash Deposit"))
        presenter.onAction(CashDepositFormUiAction.OnCurrencySelect(0))
        presenter.onAction(CashDepositFormUiAction.OnHolderNameChange("Alice Doe"))
        presenter.onAction(CashDepositFormUiAction.OnHolderIdChange("ID-123"))
        presenter.onAction(CashDepositFormUiAction.OnAccountNrChange("123456789"))
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.CASH_DEPOSIT,
            name = "Cash Deposit",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar")),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

    private fun sampleCountryDetails(bankAccountValidationSupported: Boolean): BankAccountCountryDetails =
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
}
