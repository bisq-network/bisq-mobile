package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AchTransferFormPresenterTest : ClientKoinIntegrationTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: AchTransferFormPresenter

    override fun onSetup() {
        presenter = AchTransferFormPresenter(mainPresenter = mainPresenter)
    }

    override fun onTearDown() {
        try {
            if (::presenter.isInitialized) presenter.onDestroy()
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `when holder name changes then updates holderNameEntry`() =
        runTest {
            presenter.onAction(AchTransferFormUiAction.OnHolderNameChange("John Doe"))
            assertEquals("John Doe", presenter.uiState.value.holderNameEntry.value)
        }

    @Test
    fun `when holder address changes then updates holderAddressEntry`() =
        runTest {
            presenter.onAction(AchTransferFormUiAction.OnHolderAddressChange("123 Main St"))
            assertEquals("123 Main St", presenter.uiState.value.holderAddressEntry.value)
        }

    @Test
    fun `when bank name changes then updates bankNameEntry`() =
        runTest {
            presenter.onAction(AchTransferFormUiAction.OnBankNameChange("Bisq Bank"))
            assertEquals("Bisq Bank", presenter.uiState.value.bankNameEntry.value)
        }

    @Test
    fun `when routing number changes then updates routingNrEntry`() =
        runTest {
            presenter.onAction(AchTransferFormUiAction.OnRoutingNrChange("021000021"))
            assertEquals("021000021", presenter.uiState.value.routingNrEntry.value)
        }

    @Test
    fun `when account number changes then updates accountNrEntry`() =
        runTest {
            presenter.onAction(AchTransferFormUiAction.OnAccountNrChange("123456789"))
            assertEquals("123456789", presenter.uiState.value.accountNrEntry.value)
        }

    @Test
    fun `when bank account type selected then updates selection and clears error`() =
        runTest {
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            assertNotNull(presenter.uiState.value.bankAccountTypeErrorMessage)

            presenter.onAction(AchTransferFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))

            assertEquals(BankAccountType.CHECKING, presenter.uiState.value.selectedBankAccountType)
            assertNull(presenter.uiState.value.bankAccountTypeErrorMessage)
        }

    @Test
    fun `when next clicked with invalid fields then no effect and errors are set`() =
        runTest {
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(AchTransferFormUiAction.OnHolderNameChange("a"))
            presenter.onAction(AchTransferFormUiAction.OnHolderAddressChange("a"))
            presenter.onAction(AchTransferFormUiAction.OnBankNameChange("a"))
            presenter.onAction(AchTransferFormUiAction.OnRoutingNrChange(""))
            presenter.onAction(AchTransferFormUiAction.OnAccountNrChange(""))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertNotNull(presenter.uniqueAccountNameEntry.value.errorMessage)
            assertNotNull(presenter.uiState.value.holderNameEntry.errorMessage)
            assertNotNull(presenter.uiState.value.holderAddressEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankNameEntry.errorMessage)
            assertNotNull(presenter.uiState.value.routingNrEntry.errorMessage)
            assertNotNull(presenter.uiState.value.accountNrEntry.errorMessage)
            assertNotNull(presenter.uiState.value.bankAccountTypeErrorMessage)
        }

    @Test
    fun `when next clicked with valid fields then emits ACH transfer account payload`() =
        runTest {
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("ACH Main"))
            presenter.onAction(AchTransferFormUiAction.OnHolderNameChange(" John Doe "))
            presenter.onAction(AchTransferFormUiAction.OnHolderAddressChange(" 123 Main St "))
            presenter.onAction(AchTransferFormUiAction.OnBankNameChange(" Bisq Bank "))
            presenter.onAction(AchTransferFormUiAction.OnRoutingNrChange(" 021000021 "))
            presenter.onAction(AchTransferFormUiAction.OnAccountNrChange(" 123456789 "))
            presenter.onAction(AchTransferFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            val effect = effectDeferred.await()
            assertTrue(effect is AchTransferFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("ACH Main", account.accountName)
            assertEquals("John Doe", account.accountPayload.holderName)
            assertEquals("123 Main St", account.accountPayload.holderAddress)
            assertEquals("Bisq Bank", account.accountPayload.bankName)
            assertEquals("021000021", account.accountPayload.routingNr)
            assertEquals("123456789", account.accountPayload.accountNr)
            assertEquals(BankAccountType.CHECKING, account.accountPayload.bankAccountType)
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
    fun `validate holder address accepts valid trimmed value`() {
        assertNull(validateHolderAddress("  123 Main St  "))
    }

    @Test
    fun `validate holder address rejects too short value`() {
        assertNotNull(validateHolderAddress("a"))
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
    fun `validate routing number accepts valid trimmed value`() {
        assertNull(validateRoutingNr("  021000021  "))
    }

    @Test
    fun `validate routing number rejects too long value`() {
        assertNotNull(validateRoutingNr("1".repeat(51)))
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
    fun `validate account number rejects too long value`() {
        assertNotNull(validateAccountNr("1".repeat(51)))
    }
}
