package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WiseFormPresenterTest : ClientKoinIntegrationTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: WiseFormPresenter

    override fun onSetup() {
        presenter = WiseFormPresenter(mainPresenter = mainPresenter)
        presenter.initialize(samplePaymentMethod())
    }

    @Test
    fun `initialize parses currencies and selects all by default`() {
        val state = presenter.uiState.value
        assertEquals(3, state.availableCurrencies.size)
        assertEquals(setOf("USD", "EUR", "GBP"), state.selectedCurrencyCodes)
    }

    @Test
    fun `when holder name changes then updates holderNameEntry`() =
        runTest {
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            assertEquals("John Doe", presenter.uiState.value.holderNameEntry.value)
        }

    @Test
    fun `when email changes then updates emailEntry`() =
        runTest {
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            assertEquals("john@example.com", presenter.uiState.value.emailEntry.value)
        }

    @Test
    fun `when toggle currency then updates selected set`() =
        runTest {
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("EUR"))
            assertEquals(setOf("USD", "GBP"), presenter.uiState.value.selectedCurrencyCodes)
        }

    @Test
    fun `when clear all then selected currencies become empty`() =
        runTest {
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            assertTrue(
                presenter.uiState.value.selectedCurrencyCodes
                    .isEmpty(),
            )
        }

    @Test
    fun `when select all then selected currencies include all available`() =
        runTest {
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            presenter.onAction(WiseFormUiAction.OnSelectAllCurrencies)
            assertEquals(setOf("USD", "EUR", "GBP"), presenter.uiState.value.selectedCurrencyCodes)
        }

    @Test
    fun `when next clicked with invalid fields then no effect and errors are set`() =
        runTest {
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("a"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("invalid"))
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertTrue(presenter.uniqueAccountNameEntry.value.errorMessage != null)
            assertTrue(presenter.uiState.value.holderNameEntry.errorMessage != null)
            assertTrue(presenter.uiState.value.emailEntry.errorMessage != null)
            assertTrue(presenter.uiState.value.currencyErrorMessage != null)
        }

    @Test
    fun `when next clicked with only unsupported selected currency codes then no effect and currency error is set`() =
        runTest {
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Wise Personal"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("UNSUPPORTED"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertTrue(presenter.uiState.value.currencyErrorMessage != null)
        }

    @Test
    fun `when next clicked with valid fields then emits Wise account payload`() =
        runTest {
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Wise Personal"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("USD"))
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("EUR"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            val effect = effectDeferred.await()
            assertTrue(effect is WiseFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Wise Personal", account.accountName)
            assertEquals("John Doe", account.accountPayload.holderName)
            assertEquals("john@example.com", account.accountPayload.email)
            assertEquals(listOf("EUR", "USD"), account.accountPayload.selectedCurrencies.map { currency -> currency.code })
        }

    @Test
    fun `validate holder name accepts trimmed valid value`() {
        assertNull(validateHolderName("  John Doe  "))
    }

    @Test
    fun `validate holder name rejects too short value`() {
        assertTrue(validateHolderName("a") != null)
    }

    @Test
    fun `validate email accepts valid value`() {
        assertNull(validateEmail("john@example.com"))
    }

    @Test
    fun `validate email rejects invalid value`() {
        assertTrue(validateEmail("bad") != null)
    }

    @Test
    fun `validate selected currencies rejects empty list`() {
        assertTrue(validateSelectedCurrencies(emptyList()) != null)
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.WISE,
            name = "Wise",
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "Pound Sterling"),
                ),
            supportedCountries = listOf(Country(code = "GB", name = "United Kingdom")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )
}
