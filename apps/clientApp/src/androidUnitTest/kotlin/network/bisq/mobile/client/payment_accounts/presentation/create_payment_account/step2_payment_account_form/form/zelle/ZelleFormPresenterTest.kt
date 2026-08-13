package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ZelleFormPresenterTest : ClientKoinIntegrationTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: ZelleFormPresenter

    override fun onSetup() {
        presenter = ZelleFormPresenter(mainPresenter = mainPresenter)
    }

    @Test
    fun `when holder name changes then updates holderNameEntry`() =
        runTest {
            // When
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))

            // Then
            assertEquals("John Doe", presenter.uiState.value.holderNameEntry.value)
        }

    @Test
    fun `when email or mobile changes then updates emailOrMobileNrEntry`() =
        runTest {
            // When
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("user@example.com"))

            // Then
            assertEquals("user@example.com", presenter.uiState.value.emailOrMobileNrEntry.value)
        }

    @Test
    fun `when next clicked with invalid entries then no navigation effect is emitted and errors are set`() =
        runTest {
            // Given
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("a"))
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("invalid"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()

            val state = presenter.uiState.value
            assertTrue(presenter.uniqueAccountNameEntry.value.errorMessage != null)
            assertTrue(state.holderNameEntry.errorMessage != null)
            assertTrue(state.emailOrMobileNrEntry.errorMessage != null)
        }

    @Test
    fun `when next clicked with valid email flow then emits account payload`() =
        runTest {
            // Given
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Personal"))
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("user@example.com"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is ZelleFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Zelle Personal", account.accountName)
            assertEquals("John Doe", account.accountPayload.holderName)
            assertEquals("user@example.com", account.accountPayload.emailOrMobileNr)
        }

    @Test
    fun `when next clicked with valid us mobile flow then emits account payload`() =
        runTest {
            // Given
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Mobile"))
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("Jane Doe"))
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("+1 202-555-0171"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is ZelleFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Zelle Mobile", account.accountName)
            assertEquals("Jane Doe", account.accountPayload.holderName)
            assertEquals("+1 202-555-0171", account.accountPayload.emailOrMobileNr)
        }

    @Test
    fun `validateHolderName accepts valid trimmed holder name`() {
        assertNull(validateHolderName("  John Doe  "))
    }

    @Test
    fun `validateHolderName rejects too short value`() {
        assertTrue(validateHolderName("a") != null)
    }

    @Test
    fun `validateEmailOrMobile accepts valid email`() {
        assertNull(validateEmailOrMobile("user@example.com"))
    }

    @Test
    fun `validateEmailOrMobile accepts valid us mobile`() {
        assertNull(validateEmailOrMobile("+1 202-555-0171"))
    }

    @Test
    fun `validateEmailOrMobile rejects invalid value`() {
        assertTrue(validateEmailOrMobile("not-an-email-or-phone") != null)
    }
}
