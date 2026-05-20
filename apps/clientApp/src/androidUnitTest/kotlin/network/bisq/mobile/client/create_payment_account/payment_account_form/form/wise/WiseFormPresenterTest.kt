package network.bisq.mobile.client.create_payment_account.payment_account_form.form.wise

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
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.WiseFormUiAction
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WiseFormPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: WiseFormPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mainPresenter = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }

        presenter = WiseFormPresenter(mainPresenter = mainPresenter)
        presenter.initialize(samplePaymentMethod())
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `initialize parses currencies and selects all by default`() {
        val state = presenter.uiState.value
        assertEquals(3, state.availableCurrencies.size)
        assertEquals(setOf("USD", "EUR", "GBP"), state.selectedCurrencyCodes)
    }

    @Test
    fun `when holder name changes then updates holderNameEntry`() =
        runTest(testDispatcher) {
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            assertEquals("John Doe", presenter.uiState.value.holderNameEntry.value)
        }

    @Test
    fun `when email changes then updates emailEntry`() =
        runTest(testDispatcher) {
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            assertEquals("john@example.com", presenter.uiState.value.emailEntry.value)
        }

    @Test
    fun `when toggle currency then updates selected set`() =
        runTest(testDispatcher) {
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("EUR"))
            assertEquals(setOf("USD", "GBP"), presenter.uiState.value.selectedCurrencyCodes)
        }

    @Test
    fun `when clear all then selected currencies become empty`() =
        runTest(testDispatcher) {
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            assertTrue(
                presenter.uiState.value.selectedCurrencyCodes
                    .isEmpty(),
            )
        }

    @Test
    fun `when select all then selected currencies include all available`() =
        runTest(testDispatcher) {
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            presenter.onAction(WiseFormUiAction.OnSelectAllCurrencies)
            assertEquals(setOf("USD", "EUR", "GBP"), presenter.uiState.value.selectedCurrencyCodes)
        }

    @Test
    fun `when next clicked with invalid fields then no effect and errors are set`() =
        runTest(testDispatcher) {
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("a"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("invalid"))
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)

            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
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
        runTest(testDispatcher) {
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Wise Personal"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("UNSUPPORTED"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertTrue(presenter.uiState.value.currencyErrorMessage != null)
        }

    @Test
    fun `when next clicked with valid fields then emits Wise account payload`() =
        runTest(testDispatcher) {
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Wise Personal"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            presenter.onAction(WiseFormUiAction.OnClearAllCurrencies)
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("USD"))
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("EUR"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
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
