package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import kotlin.test.assertEquals

data class TradeDetailsHeaderTradeStateExpectation(
    val state: BisqEasyTradeStateEnum,
    val expectedCloseType: TradeDetailsHeaderPresenter.TradeCloseType?,
    val interruptI18nKey: String?,
    val mediationI18nKey: String?,
    val expectSessionCompleted: Boolean,
) {
    override fun toString(): String = state.name
}

/**
 * One row per representative [BisqEasyTradeStateEnum] value per [TradeDetailsHeaderPresenter.tradeStateChanged] branch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class TradeDetailsHeaderPresenterTradeStateParameterizedTest(
    private val expectation: TradeDetailsHeaderTradeStateExpectation,
) : PresentationKoinTestBase() {
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
        I18nSupport.initialize("en")
        GenericErrorHandler.clearGenericError()

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

    private fun createPresenter(): TradeDetailsHeaderPresenter =
        TradeDetailsHeaderPresenter(
            mainPresenter,
            tradesServiceFacade,
            mediationServiceFacade,
            userProfileServiceFacade,
        )

    @Test
    fun tradeState_setsExpectedHeaderActions() {
        runTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = true)
            harness.tradeStateFlow.value = expectation.state
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(expectation.expectedCloseType, presenter.tradeCloseType.value, expectation.state.name)
            val expectedInterrupt = expectation.interruptI18nKey?.i18n() ?: ""
            val expectedMediation = expectation.mediationI18nKey?.i18n() ?: ""
            assertEquals(expectedInterrupt, presenter.interruptTradeButtonText.value, expectation.state.name)
            assertEquals(expectedMediation, presenter.openMediationButtonText.value, expectation.state.name)
            assertEquals(expectation.expectSessionCompleted, presenter.sessionUiState.value.isCompleted, expectation.state.name)
            assertEquals(expectedInterrupt, presenter.sessionUiState.value.interruptTradeButtonText, expectation.state.name)
            assertEquals(expectedMediation, presenter.sessionUiState.value.openMediationButtonText, expectation.state.name)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun expectations(): List<Array<TradeDetailsHeaderTradeStateExpectation>> =
            listOf(
                // Reject + report-to-mediator bucket
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.INIT,
                    TradeDetailsHeaderPresenter.TradeCloseType.REJECT,
                    "bisqEasy.openTrades.rejectTrade",
                    "bisqEasy.tradeState.reportToMediator",
                    false,
                ),
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
                    TradeDetailsHeaderPresenter.TradeCloseType.REJECT,
                    "bisqEasy.openTrades.rejectTrade",
                    "bisqEasy.tradeState.reportToMediator",
                    false,
                ),
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                    TradeDetailsHeaderPresenter.TradeCloseType.REJECT,
                    "bisqEasy.openTrades.rejectTrade",
                    "bisqEasy.tradeState.reportToMediator",
                    false,
                ),
                // Cancel + request-mediation bucket
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION,
                    TradeDetailsHeaderPresenter.TradeCloseType.CANCEL,
                    "bisqEasy.openTrades.cancelTrade",
                    "bisqEasy.tradeState.requestMediation",
                    false,
                ),
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
                    TradeDetailsHeaderPresenter.TradeCloseType.CANCEL,
                    "bisqEasy.openTrades.cancelTrade",
                    "bisqEasy.tradeState.requestMediation",
                    false,
                ),
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
                    TradeDetailsHeaderPresenter.TradeCloseType.CANCEL,
                    "bisqEasy.openTrades.cancelTrade",
                    "bisqEasy.tradeState.requestMediation",
                    false,
                ),
                // Completed
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.BTC_CONFIRMED,
                    TradeDetailsHeaderPresenter.TradeCloseType.COMPLETED,
                    null,
                    null,
                    true,
                ),
                // Terminal: no close type, no action labels
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.REJECTED,
                    null,
                    null,
                    null,
                    false,
                ),
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.PEER_CANCELLED,
                    null,
                    null,
                    null,
                    false,
                ),
                TradeDetailsHeaderTradeStateExpectation(
                    BisqEasyTradeStateEnum.FAILED,
                    null,
                    null,
                    null,
                    false,
                ),
            ).map { arrayOf(it) }
    }
}
