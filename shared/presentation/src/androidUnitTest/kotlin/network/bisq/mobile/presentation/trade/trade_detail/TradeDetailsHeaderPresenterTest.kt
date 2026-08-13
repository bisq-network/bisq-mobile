package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TradeDetailsHeaderPresenterTest : PresentationKoinTestBase() {
    private var originalLocale: Locale? = null

    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val mediationServiceFacade: MediationServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    override fun beforeStartKoin() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        GenericErrorHandler.clearGenericError()
        I18nSupport.initialize("en")

        every { mainPresenter.isSmallScreen } returns MutableStateFlow(false)
    }

    override fun onTearDown() {
        try {
            originalLocale?.let { Locale.setDefault(it) }
            GenericErrorHandler.clearGenericError()
        } finally {
            super.onTearDown()
        }
    }

    private fun createPresenter(settingsRepository: SettingsRepositoryMock = SettingsRepositoryMock()): TradeDetailsHeaderPresenter =
        TradeDetailsHeaderPresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            userProfileServiceFacade,
            settingsRepository,
        )

    @Test
    fun `when view attached for seller then direction is sell`() =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(DirectionEnum.SELL, presenter.directionEnum)
        }

    @Test
    fun `isAnalyticsEnabled mirrors the persisted opt-in and gates the interrupt-reason chips`() =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            val settingsRepository = SettingsRepositoryMock()

            val presenter = createPresenter(settingsRepository)
            presenter.onViewAttached()
            advanceUntilIdle()

            // Default is opted-out — the chips must never show unless proven otherwise.
            assertEquals(false, presenter.isAnalyticsEnabled.value)

            settingsRepository.setAnalyticsEnabled(true)
            advanceUntilIdle()
            assertEquals(true, presenter.isAnalyticsEnabled.value)

            settingsRepository.setAnalyticsEnabled(false)
            advanceUntilIdle()
            assertEquals(false, presenter.isAnalyticsEnabled.value)
        }

    @Test
    fun `when view attached for buyer then direction is buy`() =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(DirectionEnum.BUY, presenter.directionEnum)
        }

    @Test
    fun `when mediation and payment data change then session state reflects them`() =
        runTest {
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
    fun `when toggle header action then updates show details in session state`() =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.sessionUiState.value.showDetails)
            presenter.onAction(TradeDetailsHeaderUiAction.ToggleHeader)
            advanceUntilIdle()

            assertTrue(presenter.sessionUiState.value.showDetails)
        }

    @Test
    fun `when open interruption and mediation confirmation actions then shows dialogs`() =
        runTest {
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
        runTest {
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
        runTest {
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
    fun `when interrupt trade in reject state then forwards selected reason to reject trade`() =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { tradesServiceFacade.rejectTrade(any()) } returns Result.success(Unit)

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onInterruptTrade(AnalyticsEvent.Trade.InterruptReason.PRICE_MOVED)
            advanceUntilIdle()

            coVerify { tradesServiceFacade.rejectTrade(AnalyticsEvent.Trade.InterruptReason.PRICE_MOVED) }
            coVerify(exactly = 0) { tradesServiceFacade.cancelTrade(any()) }
        }

    @Test
    fun `when interrupt trade in cancel state then forwards selected reason to cancel trade`() =
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade
            coEvery { tradesServiceFacade.cancelTrade(any()) } returns Result.success(Unit)

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            harness.tradeStateFlow.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            advanceUntilIdle()

            presenter.onInterruptTrade(AnalyticsEvent.Trade.InterruptReason.PRICE_MOVED)
            advanceUntilIdle()

            coVerify { tradesServiceFacade.cancelTrade(AnalyticsEvent.Trade.InterruptReason.PRICE_MOVED) }
            coVerify(exactly = 0) { tradesServiceFacade.rejectTrade(any()) }
        }

    @Test
    fun `when open mediation and mediator not available then sets no mediator error`() =
        runTest {
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
        runTest {
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
        runTest {
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
        runTest {
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
        runTest {
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
        runTest {
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
