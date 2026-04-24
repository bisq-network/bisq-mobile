package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

import io.mockk.coEvery
import io.mockk.coVerify
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
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
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
class SelectFiatPaymentMethodPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private lateinit var presenter: SelectFiatPaymentMethodPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        paymentAccountsServiceFacade = mockk(relaxed = true)
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

    private fun createPresenter(): SelectFiatPaymentMethodPresenter =
        SelectFiatPaymentMethodPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initial state then has expected defaults`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            val state = presenter.uiState.value

            // Then
            assertTrue(state.paymentMethods.isEmpty())
            assertNull(state.selectedPaymentMethod)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals("", state.searchQuery)
            assertNull(state.activeRiskFilter)
        }

    @Test
    fun `when view is attached then fiat methods are loaded`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(
                        rail = FiatPaymentRail.ZELLE,
                        name = "Zelle",
                        risk = FiatPaymentMethodChargebackRisk.LOW,
                    ),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Revolut",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            val state = presenter.uiState.value
            assertEquals(2, state.paymentMethods.size)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
        }

    @Test
    fun `when fiat methods load fails then error state is set`() =
        runTest(testDispatcher) {
            // Given
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.failure(IllegalStateException("fiat fail"))
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isError)
            assertTrue(state.paymentMethods.isEmpty())
        }

    @Test
    fun `when retry is clicked after failure then methods are requested again and error clears on success`() =
        runTest(testDispatcher) {
            // Given
            val methods = listOf(sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW))
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returnsMany
                listOf(
                    Result.failure(IllegalStateException("first fail")),
                    Result.success(methods),
                )
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals(1, state.paymentMethods.size)
        }

    @Test
    fun `when search query changes then list is filtered by method name`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Revolut",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnSearchQueryChange("zelle"))

            // Then
            val state = presenter.uiState.value
            assertEquals("zelle", state.searchQuery)
            assertEquals(1, state.paymentMethods.size)
            assertEquals("Zelle", state.paymentMethods.first().name)
        }

    @Test
    fun `when risk filter changes then list is filtered by chargeback risk`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Revolut",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals(FiatPaymentMethodChargebackRiskVO.LOW, state.activeRiskFilter)
            assertEquals(1, state.paymentMethods.size)
            assertEquals("Zelle", state.paymentMethods.first().name)
        }

    @Test
    fun `when both query and risk filter are applied then fiat results satisfy both predicates`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle Fast", risk = FiatPaymentMethodChargebackRisk.LOW),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.CASH_APP,
                        name = "Cash App",
                        risk = FiatPaymentMethodChargebackRisk.LOW,
                    ),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Zelle Clone",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnSearchQueryChange("zelle"))
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.paymentMethods.size)
            assertEquals("Zelle Fast", state.paymentMethods.first().name)
        }

    @Test
    fun `when selected fiat method is filtered out by search then selected method is cleared`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Revolut",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            val zelle =
                presenter.uiState.value.paymentMethods
                    .first { it.name == "Zelle" }
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnPaymentMethodClick(zelle))

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnSearchQueryChange("revolut"))

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.paymentMethods.size)
            assertEquals("Revolut", state.paymentMethods.first().name)
            assertNull(state.selectedPaymentMethod)
        }

    @Test
    fun `when selected fiat method is filtered out by risk filter then selected method is cleared`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Revolut",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            val zelle =
                presenter.uiState.value.paymentMethods
                    .first { it.name == "Zelle" }
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnPaymentMethodClick(zelle))

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.VERY_LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals(FiatPaymentMethodChargebackRiskVO.VERY_LOW, state.activeRiskFilter)
            assertEquals(1, state.paymentMethods.size)
            assertEquals("Revolut", state.paymentMethods.first().name)
            assertNull(state.selectedPaymentMethod)
        }

    @Test
    fun `when fiat method is selected then selected payment method is updated`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            val selected =
                presenter.uiState.value.paymentMethods
                    .first()

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnPaymentMethodClick(selected))

            // Then
            val state = presenter.uiState.value
            assertEquals("Zelle", state.selectedPaymentMethod?.name)
        }

    @Test
    fun `when search query has surrounding whitespace then query is trimmed before filtering`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                    sampleFiatMethod(
                        rail = FiatPaymentRail.REVOLUT,
                        name = "Revolut",
                        risk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnSearchQueryChange("  zelle  "))

            // Then
            val state = presenter.uiState.value
            assertEquals("zelle", state.searchQuery)
            assertEquals(1, state.paymentMethods.size)
            assertEquals("Zelle", state.paymentMethods.first().name)
        }

    @Test
    fun `when next is clicked with a selected method then navigation effect is emitted`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(
                        rail = FiatPaymentRail.ZELLE,
                        name = "Zelle",
                        risk = FiatPaymentMethodChargebackRisk.LOW,
                    ),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            val selected =
                presenter.uiState.value.paymentMethods
                    .first()
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnPaymentMethodClick(selected))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(SelectFiatPaymentMethodUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is SelectFiatPaymentMethodEffect.NavigateToNextScreen)
            assertEquals("Zelle", effect.selectedPaymentMethod.name)
        }

    private fun sampleFiatMethod(
        rail: FiatPaymentRail,
        name: String,
        risk: FiatPaymentMethodChargebackRisk,
        supportedCurrencyCodes: String = "USD",
        countryNames: String = "United States",
    ): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = rail,
            name = name,
            supportedCurrencyCodes = supportedCurrencyCodes,
            countryNames = countryNames,
            chargebackRisk = risk,
        )
}
