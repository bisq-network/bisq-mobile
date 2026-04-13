package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelectPaymentMethodPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private lateinit var presenter: SelectPaymentMethodPresenter

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

    private fun createPresenter(): SelectPaymentMethodPresenter =
        SelectPaymentMethodPresenter(
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
            assertTrue(state.fiatPaymentMethods.isEmpty())
            assertTrue(state.cryptoPaymentMethods.isEmpty())
            assertNull(state.selectedFiatPaymentMethod)
            assertNull(state.selectedCryptoPaymentMethod)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals("", state.searchQuery)
            assertNull(state.activeRiskFilter)
        }

    @Test
    fun `when presenter is initialized with fiat type then it loads fiat methods and clears loading state`() =
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            coVerify(exactly = 0) { paymentAccountsServiceFacade.getCryptoPaymentMethods() }
            val state = presenter.uiState.value
            assertEquals(2, state.fiatPaymentMethods.size)
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isError)
            assertTrue(state.fiatPaymentMethods.isEmpty())
        }

    @Test
    fun `when retry is clicked after fiat failure then methods are requested again and error clears on success`() =
        runTest(testDispatcher) {
            // Given
            val methods = listOf(sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW))
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returnsMany
                listOf(
                    Result.failure(IllegalStateException("first fail")),
                    Result.success(methods),
                )
            presenter = createPresenter()
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals(1, state.fiatPaymentMethods.size)
        }

    @Test
    fun `when search query changes for fiat then list is filtered by method name`() =
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("zelle"))

            // Then
            val state = presenter.uiState.value
            assertEquals("zelle", state.searchQuery)
            assertEquals(1, state.fiatPaymentMethods.size)
            assertEquals("Zelle", state.fiatPaymentMethods.first().name)
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals(FiatPaymentMethodChargebackRiskVO.LOW, state.activeRiskFilter)
            assertEquals(1, state.fiatPaymentMethods.size)
            assertEquals("Zelle", state.fiatPaymentMethods.first().name)
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("zelle"))
            presenter.onAction(SelectPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.fiatPaymentMethods.size)
            assertEquals("Zelle Fast", state.fiatPaymentMethods.first().name)
        }

    @Test
    fun `when selected fiat method is filtered out by search then selected fiat is cleared`() =
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            val zelle =
                presenter.uiState.value.fiatPaymentMethods
                    .first { it.name == "Zelle" }
            presenter.onAction(SelectPaymentMethodUiAction.OnFiatPaymentMethodClick(zelle))

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("revolut"))

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.fiatPaymentMethods.size)
            assertEquals("Revolut", state.fiatPaymentMethods.first().name)
            assertNull(state.selectedFiatPaymentMethod)
        }

    @Test
    fun `when selected fiat method is filtered out by risk filter then selected fiat is cleared`() =
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            val zelle =
                presenter.uiState.value.fiatPaymentMethods
                    .first { it.name == "Zelle" }
            presenter.onAction(SelectPaymentMethodUiAction.OnFiatPaymentMethodClick(zelle))

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.VERY_LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals(FiatPaymentMethodChargebackRiskVO.VERY_LOW, state.activeRiskFilter)
            assertEquals(1, state.fiatPaymentMethods.size)
            assertEquals("Revolut", state.fiatPaymentMethods.first().name)
            assertNull(state.selectedFiatPaymentMethod)
        }

    @Test
    fun `when fiat method is selected then selected fiat is set and selected crypto is cleared`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleFiatMethod(rail = FiatPaymentRail.ZELLE, name = "Zelle", risk = FiatPaymentMethodChargebackRisk.LOW),
                )
            coEvery { paymentAccountsServiceFacade.getFiatPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()
            val selected =
                presenter.uiState.value.fiatPaymentMethods
                    .first()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnFiatPaymentMethodClick(selected))

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.selectedFiatPaymentMethod)
            assertEquals("Zelle", state.selectedFiatPaymentMethod.name)
            assertNull(state.selectedCryptoPaymentMethod)
        }

    @Test
    fun `when presenter is initialized with crypto type then it loads crypto methods and clears loading state`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()

            // When
            presenter.initialize(PaymentAccountType.CRYPTO)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getCryptoPaymentMethods() }
            coVerify(exactly = 0) { paymentAccountsServiceFacade.getFiatPaymentMethods() }
            val state = presenter.uiState.value
            assertEquals(2, state.cryptoPaymentMethods.size)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
        }

    @Test
    fun `when crypto methods load fails then error state is set`() =
        runTest(testDispatcher) {
            // Given
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.failure(IllegalStateException("crypto fail"))
            presenter = createPresenter()

            // When
            presenter.initialize(PaymentAccountType.CRYPTO)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getCryptoPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isError)
            assertTrue(state.cryptoPaymentMethods.isEmpty())
        }

    @Test
    fun `when search query changes for crypto then list is filtered by name or code`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.initialize(PaymentAccountType.CRYPTO)
            advanceUntilIdle()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("ltc"))

            // Then
            val state = presenter.uiState.value
            assertEquals("ltc", state.searchQuery)
            assertEquals(1, state.cryptoPaymentMethods.size)
            assertEquals("LTC", state.cryptoPaymentMethods.first().code)
        }

    @Test
    fun `when selected crypto method is filtered out by search then selected crypto is cleared`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.initialize(PaymentAccountType.CRYPTO)
            advanceUntilIdle()

            val monero =
                presenter.uiState.value.cryptoPaymentMethods
                    .first { it.code == "XMR" }
            presenter.onAction(SelectPaymentMethodUiAction.OnCryptoPaymentMethodClick(monero))

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("ltc"))

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.cryptoPaymentMethods.size)
            assertEquals("LTC", state.cryptoPaymentMethods.first().code)
            assertNull(state.selectedCryptoPaymentMethod)
        }

    @Test
    fun `when crypto method is selected then selected crypto is set and selected fiat is cleared`() =
        runTest(testDispatcher) {
            // Given
            val methods = listOf(sampleCryptoMethod(code = "XMR", name = "Monero"))
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.initialize(PaymentAccountType.CRYPTO)
            advanceUntilIdle()
            val selected =
                presenter.uiState.value.cryptoPaymentMethods
                    .first()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnCryptoPaymentMethodClick(selected))

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.selectedCryptoPaymentMethod)
            assertEquals("XMR", state.selectedCryptoPaymentMethod.code)
            assertNull(state.selectedFiatPaymentMethod)
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
            presenter.initialize(PaymentAccountType.FIAT)
            advanceUntilIdle()

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("  zelle  "))

            // Then
            val state = presenter.uiState.value
            assertEquals("zelle", state.searchQuery)
            assertEquals(1, state.fiatPaymentMethods.size)
            assertEquals("Zelle", state.fiatPaymentMethods.first().name)
        }

    @Test
    fun `when risk filter changes in crypto mode then crypto filtering remains consistent with current query`() =
        runTest(testDispatcher) {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.initialize(PaymentAccountType.CRYPTO)
            advanceUntilIdle()
            presenter.onAction(SelectPaymentMethodUiAction.OnSearchQueryChange("xmr"))

            // When
            presenter.onAction(SelectPaymentMethodUiAction.OnRiskFilterChange(FiatPaymentMethodChargebackRiskVO.LOW))

            // Then
            val state = presenter.uiState.value
            assertEquals("xmr", state.searchQuery)
            assertEquals(FiatPaymentMethodChargebackRiskVO.LOW, state.activeRiskFilter)
            assertEquals(1, state.cryptoPaymentMethods.size)
            assertEquals("XMR", state.cryptoPaymentMethods.first().code)
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

    private fun sampleCryptoMethod(
        code: String,
        name: String,
        category: String = "ALTCOIN",
    ): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = code,
            name = name,
            category = category,
        )
}
