package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TradeDetailsHeaderPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private var originalLocale: Locale? = null

    private lateinit var tradesServiceFacade: TradesServiceFacade
    private lateinit var mediationServiceFacade: MediationServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private val navigationManager: NavigationManager = mockk(relaxed = true)
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    private val testKoinModule =
        module {
            single { CoroutineExceptionHandlerSetup() }
            factory<CoroutineJobsManager> {
                DefaultCoroutineJobsManager().apply {
                    get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                }
            }
            single<NavigationManager> { navigationManager }
            single<GlobalUiManager> { globalUiManager }
        }

    @BeforeTest
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)

        Dispatchers.setMain(testDispatcher)

        tradesServiceFacade = mockk(relaxed = true)
        mediationServiceFacade = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        startKoin { modules(testKoinModule) }
        GenericErrorHandler.clearGenericError()
        I18nSupport.initialize("en")

        every { mainPresenter.languageCode } returns MutableStateFlow("en")
        every { mainPresenter.isSmallScreen } returns MutableStateFlow(false)
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
            originalLocale?.let { Locale.setDefault(it) }
            GenericErrorHandler.clearGenericError()
        }
    }

    private fun createPresenter(): TradeDetailsHeaderPresenter =
        TradeDetailsHeaderPresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            userProfileServiceFacade,
        )

    @Test
    fun `when view attached for seller then direction is sell`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(DirectionEnum.SELL, presenter.directionEnum)
        }

    @Test
    fun `when view attached for buyer then direction is buy`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(DirectionEnum.BUY, presenter.directionEnum)
        }

    @Test
    fun `when mediation and payment data change then session state reflects them`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.sessionUiState.value.isInMediation)
            assertNull(presenter.sessionUiState.value.paymentProof)
            assertNull(presenter.sessionUiState.value.receiverAddress)

            harness.isInMediationFlow.value = true
            harness.paymentProofFlow.value = "proof-tx"
            harness.bitcoinPaymentDataFlow.value = "bc1qaddr"
            advanceUntilIdle()

            assertTrue(presenter.sessionUiState.value.isInMediation)
            assertEquals("proof-tx", presenter.sessionUiState.value.paymentProof)
            assertEquals("bc1qaddr", presenter.sessionUiState.value.receiverAddress)
        }

    @Test
    fun `when toggle header action then updates show details and interactivity in session state`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.sessionUiState.value.isInteractive)

            assertFalse(presenter.sessionUiState.value.showDetails)
            presenter.onAction(TradeDetailsHeaderUiAction.ToggleHeader)
            // Flush disableInteractive + combine; do not advance time — advanceUntilIdle can complete
            // enableInteractive()'s delayed re-enable before we assert the brief non-interactive window.
            runCurrent()

            assertTrue(presenter.sessionUiState.value.showDetails)
            assertFalse(presenter.sessionUiState.value.isInteractive)

            advanceTimeBy(BasePresenter.SMALLEST_PERCEPTIVE_DELAY)
            runCurrent()

            assertTrue(presenter.sessionUiState.value.isInteractive)
        }

    @Test
    fun `when open interruption and mediation confirmation actions then shows dialogs`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(TradeDetailsHeaderUiAction.OpenInterruptionConfirmationDialog)
            advanceUntilIdle()
            assertTrue(presenter.showInterruptionConfirmationDialog.value)

            presenter.onAction(TradeDetailsHeaderUiAction.OpenMediationConfirmationDialog)
            advanceUntilIdle()
            assertTrue(presenter.showMediationConfirmationDialog.value)
        }

    @Test
    fun `when interrupt trade in reject state then calls reject trade`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { tradesServiceFacade.rejectTrade() } returns Result.success(Unit)

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onInterruptTrade()
            advanceUntilIdle()

            coVerify { tradesServiceFacade.rejectTrade() }
        }

    @Test
    fun `when interrupt trade in cancel state then calls cancel trade`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { tradesServiceFacade.cancelTrade() } returns Result.success(Unit)

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            harness.tradeStateFlow.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            advanceUntilIdle()

            presenter.onInterruptTrade()
            advanceUntilIdle()

            coVerify { tradesServiceFacade.cancelTrade() }
        }

    @Test
    fun `when open mediation and mediator not available then sets no mediator error`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { mediationServiceFacade.reportToMediator(any()) } returns
                Result.failure(MediatorNotAvailableException())

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onOpenMediation()
            advanceUntilIdle()

            assertEquals(
                "mobile.takeOffer.noMediatorAvailable.warning".i18n(),
                presenter.mediationError.value,
            )
        }

    @Test
    fun `when open mediation fails then sets mediation failed error`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { mediationServiceFacade.reportToMediator(any()) } returns
                Result.failure(RuntimeException("x"))

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onOpenMediation()
            advanceUntilIdle()

            assertEquals(
                "mobile.bisqEasy.tradeState.mediationFailed".i18n(),
                presenter.mediationError.value,
            )
        }

    @Test
    fun `when open mediation with null selected trade then sets mediation failed error`() =
        runTest(testDispatcher) {
            val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
            every { tradesServiceFacade.selectedTrade } returns selected

            val presenter = createPresenter()
            // Avoid require(selected != null) — only exercise onOpenMediation without attach
            presenter.onOpenMediation()

            assertEquals(
                "mobile.bisqEasy.tradeState.mediationFailed".i18n(),
                presenter.mediationError.value,
            )
        }

    @Test
    fun `when trade completed date is set then session state shows formatted trade duration`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals("", presenter.sessionUiState.value.formattedTradeDuration)

            val takeOfferDate = 1_000_000L
            val completedAt = takeOfferDate + 90_000L
            harness.tradeCompletedDateFlow.value = completedAt
            advanceUntilIdle()

            assertEquals("1 min, 30 sec", presenter.sessionUiState.value.formattedTradeDuration)
        }

    @Test
    fun `when selected trade becomes null then clears trade ui state`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertNotNull(presenter.tradeUiState.value)
            harness.selectedTrade.value = null
            advanceUntilIdle()

            assertNull(presenter.tradeUiState.value)
            assertEquals("", presenter.sessionUiState.value.formattedTradeDuration)
        }

    @Test
    fun `when view unattaching then resets presenter state`() =
        runTest(testDispatcher) {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { mediationServiceFacade.reportToMediator(any()) } returns
                Result.failure(RuntimeException("x"))

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(TradeDetailsHeaderUiAction.OpenInterruptionConfirmationDialog)
            advanceUntilIdle()
            assertTrue(presenter.showInterruptionConfirmationDialog.value)

            presenter.onOpenMediation()
            advanceUntilIdle()
            assertTrue(presenter.mediationError.value.isNotEmpty())

            presenter.onAction(TradeDetailsHeaderUiAction.ToggleHeader)
            advanceUntilIdle()
            assertTrue(presenter.sessionUiState.value.showDetails)

            presenter.onViewUnattaching()
            advanceUntilIdle()

            assertNull(presenter.tradeUiState.value)
            assertEquals(TradeDetailsHeaderSessionUiState(), presenter.sessionUiState.value)
            assertFalse(presenter.showInterruptionConfirmationDialog.value)
            assertFalse(presenter.showMediationConfirmationDialog.value)
            assertEquals("", presenter.mediationError.value)
            assertFalse(presenter.sessionUiState.value.showDetails)
        }
}
