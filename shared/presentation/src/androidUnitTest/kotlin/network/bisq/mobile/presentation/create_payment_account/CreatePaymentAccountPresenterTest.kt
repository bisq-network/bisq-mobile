package network.bisq.mobile.presentation.create_payment_account

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
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePaymentAccountPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: CreatePaymentAccountPresenter

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
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): CreatePaymentAccountPresenter =
        CreatePaymentAccountPresenter(
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initial state then payment method and payment account are null`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            val state = presenter.uiState.value

            // Then
            assertNull(state.paymentMethod)
            assertNull(state.paymentAccount)
        }

    @Test
    fun `when navigate from select payment method then updates method and emits form navigation effect`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            val paymentMethod = sampleFiatPaymentMethod(name = "SEPA")

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromSelectPaymentMethod(paymentMethod))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(paymentMethod, state.paymentMethod)
            assertNull(state.paymentAccount)

            val effect = effectDeferred.await()
            assertTrue(effect is CreatePaymentAccountEffect.NavigateToPaymentAccountForm)
        }

    @Test
    fun `when navigate from payment account form then updates account and emits review navigation effect`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            val paymentAccount = sampleUserDefinedFiatAccount(accountName = "My Account")

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromPaymentAccountForm(paymentAccount))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNull(state.paymentMethod)
            assertEquals(paymentAccount, state.paymentAccount)

            val effect = effectDeferred.await()
            assertTrue(effect is CreatePaymentAccountEffect.NavigateToPaymentAccountReview)
        }

    @Test
    fun `when select method then form submitted then state retains both values and effects are emitted in order`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            val paymentMethod = sampleFiatPaymentMethod(name = "SEPA")
            val paymentAccount = sampleUserDefinedFiatAccount(accountName = "SEPA Personal")

            // When
            val firstEffectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromSelectPaymentMethod(paymentMethod))
            advanceUntilIdle()

            val firstEffect = firstEffectDeferred.await()
            assertTrue(firstEffect is CreatePaymentAccountEffect.NavigateToPaymentAccountForm)

            val secondEffectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromPaymentAccountForm(paymentAccount))
            advanceUntilIdle()

            // Then
            val secondEffect = secondEffectDeferred.await()
            assertTrue(secondEffect is CreatePaymentAccountEffect.NavigateToPaymentAccountReview)

            val state = presenter.uiState.value
            assertEquals(paymentMethod, state.paymentMethod)
            assertEquals(paymentAccount, state.paymentAccount)
        }

    private fun sampleFiatPaymentMethod(
        name: String,
        paymentType: PaymentTypeVO = PaymentTypeVO.SEPA,
    ): FiatPaymentMethodVO =
        FiatPaymentMethodVO(
            paymentType = paymentType,
            name = name,
            supportedCurrencyCodes = "EUR",
            countryNames = "Germany",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
            restrictions = EMPTY_STRING,
        )

    private fun sampleUserDefinedFiatAccount(accountName: String): UserDefinedFiatAccount =
        UserDefinedFiatAccount(
            accountName = accountName,
            accountPayload = UserDefinedFiatAccountPayload(accountData = "sample-data"),
        )
}
