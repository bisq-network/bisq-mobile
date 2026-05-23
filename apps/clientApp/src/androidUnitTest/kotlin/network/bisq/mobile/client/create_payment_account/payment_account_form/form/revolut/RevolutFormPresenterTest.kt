package network.bisq.mobile.client.create_payment_account.payment_account_form.form.revolut

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
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.RevolutFormUiAction
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
class RevolutFormPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: RevolutFormPresenter

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

        presenter = RevolutFormPresenter(mainPresenter = mainPresenter)
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
    fun `when username changes then updates userNameEntry`() =
        runTest(testDispatcher) {
            presenter.onAction(RevolutFormUiAction.OnUserNameChange("satoshi"))
            assertEquals("satoshi", presenter.uiState.value.userNameEntry.value)
        }

    @Test
    fun `when currency picker opens and closes then state updates and search clears`() =
        runTest(testDispatcher) {
            presenter.onAction(RevolutFormUiAction.OnOpenCurrencyPicker)
            presenter.onAction(RevolutFormUiAction.OnCurrencySearchChange("eur"))
            assertTrue(presenter.uiState.value.isCurrencyPickerOpen)
            assertEquals("eur", presenter.uiState.value.currencySearchQuery)

            presenter.onAction(RevolutFormUiAction.OnCloseCurrencyPicker)
            assertFalse(presenter.uiState.value.isCurrencyPickerOpen)
            assertEquals("", presenter.uiState.value.currencySearchQuery)
        }

    @Test
    fun `when toggle currency then updates selected set`() =
        runTest(testDispatcher) {
            presenter.onAction(RevolutFormUiAction.OnCurrencyToggle("EUR"))
            assertEquals(setOf("USD", "GBP"), presenter.uiState.value.selectedCurrencyCodes)
        }

    @Test
    fun `when clear all then selected currencies become empty`() =
        runTest(testDispatcher) {
            presenter.onAction(RevolutFormUiAction.OnClearAllCurrencies)
            assertTrue(
                presenter.uiState.value.selectedCurrencyCodes
                    .isEmpty(),
            )
        }

    @Test
    fun `when select all then selected currencies include all available`() =
        runTest(testDispatcher) {
            presenter.onAction(RevolutFormUiAction.OnClearAllCurrencies)
            presenter.onAction(RevolutFormUiAction.OnSelectAllCurrencies)
            assertEquals(setOf("USD", "EUR", "GBP"), presenter.uiState.value.selectedCurrencyCodes)
        }

    @Test
    fun `when next clicked with invalid fields then no effect and errors are set`() =
        runTest(testDispatcher) {
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(RevolutFormUiAction.OnUserNameChange("a"))
            presenter.onAction(RevolutFormUiAction.OnClearAllCurrencies)

            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertTrue(presenter.uniqueAccountNameEntry.value.errorMessage != null)
            assertTrue(presenter.uiState.value.userNameEntry.errorMessage != null)
            assertTrue(presenter.uiState.value.currencyErrorMessage != null)
        }

    @Test
    fun `when next clicked with only unsupported selected currency codes then no effect and currency error is set`() =
        runTest(testDispatcher) {
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Revolut Personal"))
            presenter.onAction(RevolutFormUiAction.OnUserNameChange("satoshi"))
            presenter.onAction(RevolutFormUiAction.OnClearAllCurrencies)
            presenter.onAction(RevolutFormUiAction.OnCurrencyToggle("UNSUPPORTED"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
            assertTrue(presenter.uiState.value.currencyErrorMessage != null)
        }

    @Test
    fun `when next clicked with valid fields then emits Revolut account payload`() =
        runTest(testDispatcher) {
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Revolut Personal"))
            presenter.onAction(RevolutFormUiAction.OnUserNameChange("  satoshi  "))
            presenter.onAction(RevolutFormUiAction.OnClearAllCurrencies)
            presenter.onAction(RevolutFormUiAction.OnCurrencyToggle("USD"))
            presenter.onAction(RevolutFormUiAction.OnCurrencyToggle("EUR"))

            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            val effect = effectDeferred.await()
            assertTrue(effect is RevolutFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Revolut Personal", account.accountName)
            assertEquals("satoshi", account.accountPayload.userName)
            assertEquals(listOf("EUR", "USD"), account.accountPayload.selectedCurrencies.map { currency -> currency.code })
        }

    @Test
    fun `validate username accepts trimmed valid value`() {
        assertNull(validateUserName("  satoshi  "))
    }

    @Test
    fun `validate username rejects too short value`() {
        assertTrue(validateUserName("a") != null)
    }

    @Test
    fun `validate selected currencies rejects empty list`() {
        assertTrue(validateSelectedCurrencies(emptyList()) != null)
    }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.REVOLUT,
            name = "Revolut",
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
