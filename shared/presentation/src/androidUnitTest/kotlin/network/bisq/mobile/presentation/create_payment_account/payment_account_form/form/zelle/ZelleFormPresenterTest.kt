package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

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
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.ZelleFormUiAction
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
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
class ZelleFormPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: ZelleFormPresenter

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

        presenter = ZelleFormPresenter(mainPresenter = mainPresenter)
        presenter.initialize(
            FiatPaymentMethodVO(
                paymentType = PaymentTypeVO.ZELLE,
                name = "Zelle",
                supportedCurrencyCodes = "USD",
                countryNames = "United States",
                chargebackRisk = FiatPaymentMethodChargebackRiskVO.MODERATE,
                tradeLimitInfo = "5000.00",
                tradeDuration = "4 days",
            ),
        )
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
    fun `when holder name changes then updates holderNameEntry`() =
        runTest(testDispatcher) {
            // When
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))

            // Then
            assertEquals("John Doe", presenter.uiState.value.holderNameEntry.value)
        }

    @Test
    fun `when email or mobile changes then updates emailOrMobileNrEntry`() =
        runTest(testDispatcher) {
            // When
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("user@example.com"))

            // Then
            assertEquals("user@example.com", presenter.uiState.value.emailOrMobileNrEntry.value)
        }

    @Test
    fun `when next clicked with invalid entries then no navigation effect is emitted and errors are set`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("a"))
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("invalid"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
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
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Personal"))
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("user@example.com"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is ZelleFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Zelle Personal", account.accountName)
            assertEquals("John Doe", account.accountPayload.holderName)
            assertEquals("user@example.com", account.accountPayload.emailOrMobileNr)
            assertNull(account.creationDate)
            assertEquals("5000.00", account.tradeLimitInfo)
            assertEquals("4 days", account.tradeDuration)
        }

    @Test
    fun `when next clicked with valid us mobile flow then emits account payload`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Mobile"))
            presenter.onAction(ZelleFormUiAction.OnHolderNameChange("Jane Doe"))
            presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("+1 202-555-0171"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is ZelleFormEffect.NavigateToNextScreen)
            val account = effect.account
            assertEquals("Zelle Mobile", account.accountName)
            assertEquals("Jane Doe", account.accountPayload.holderName)
            assertEquals("+1 202-555-0171", account.accountPayload.emailOrMobileNr)
            assertNull(account.creationDate)
            assertEquals("5000.00", account.tradeLimitInfo)
            assertEquals("4 days", account.tradeDuration)
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
