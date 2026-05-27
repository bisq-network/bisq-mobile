package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account

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
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccountPayload
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePaymentAccountPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: CreatePaymentAccountPresenter

    val mockFiatPaymentMethod: FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.ZELLE,
            name = "Zelle",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar")),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "1 day",
        )
    val mockCreateAccount =
        CreateZelleAccount(
            accountName = "Zelle Personal",
            accountPayload =
                CreateZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                ),
        )

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
    fun `when initial state then payment method and create account are null`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            val state = presenter.uiState.value

            // Then
            assertNull(state.paymentMethod)
            assertNull(state.createPaymentAccount)
        }

    @Test
    fun `when navigate from select payment method then updates method and emits form navigation effect`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            val paymentMethod = mockFiatPaymentMethod

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromSelectPaymentMethod(paymentMethod))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(paymentMethod, state.paymentMethod)
            assertNull(state.createPaymentAccount)

            val effect = effectDeferred.await()
            assertTrue(effect is CreatePaymentAccountEffect.NavigateToPaymentAccountForm)
        }

    @Test
    fun `when navigate from payment account form then updates create account and emits review navigation effect`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            val createAccount = mockCreateAccount

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromPaymentAccountForm(createAccount))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNull(state.paymentMethod)
            assertEquals(createAccount, state.createPaymentAccount)

            val effect = effectDeferred.await()
            assertTrue(effect is CreatePaymentAccountEffect.NavigateToPaymentAccountReview)
        }

    @Test
    fun `when select method then form submitted then state retains both values and effects are emitted in order`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            val paymentMethod = mockFiatPaymentMethod
            val createAccount = mockCreateAccount

            // When
            val firstEffectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromSelectPaymentMethod(paymentMethod))
            advanceUntilIdle()

            val firstEffect = firstEffectDeferred.await()
            assertTrue(firstEffect is CreatePaymentAccountEffect.NavigateToPaymentAccountForm)

            val secondEffectDeferred = async { presenter.effect.first() }
            presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromPaymentAccountForm(createAccount))
            advanceUntilIdle()

            // Then
            val secondEffect = secondEffectDeferred.await()
            assertTrue(secondEffect is CreatePaymentAccountEffect.NavigateToPaymentAccountReview)

            val state = presenter.uiState.value
            assertEquals(paymentMethod, state.paymentMethod)
            assertEquals(createAccount, state.createPaymentAccount)
        }
}
