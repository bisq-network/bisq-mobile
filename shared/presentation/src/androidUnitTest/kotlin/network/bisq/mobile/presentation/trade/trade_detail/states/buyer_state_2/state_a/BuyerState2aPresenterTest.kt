package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BuyerState2aPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val peer: UserProfileVO = mockk(relaxed = true)
    private val paymentAccountData = MutableStateFlow<String?>(ACCOUNT_DATA)
    private val selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)

    private fun trade(
        accountData: StateFlow<String?> = paymentAccountData,
        id: String = "trade-1",
    ): TradeItemPresentationModel =
        mockk(relaxed = true) {
            every { tradeId } returns id
            every { quoteAmountWithCode } returns "100 USD"
            every { bisqEasyTradeModel.paymentAccountData } returns accountData
            every { bisqEasyTradeModel.shortId } returns "abc123"
            every { peersUserProfile } returns peer
            every { peersUserName } returns "seller"
        }

    private fun givenTrade(isBanned: Boolean = false): BuyerState2aPresenter {
        selectedTrade.value = trade()
        every { tradesServiceFacade.selectedTrade } returns selectedTrade
        coEvery { tradesServiceFacade.isAccountDataBanned(ACCOUNT_DATA) } returns isBanned
        return BuyerState2aPresenter(mainPresenter, tradesServiceFacade, userProfileServiceFacade)
    }

    @Test
    fun `account data not banned enables confirm without side effects`() =
        runTest {
            val presenter = givenTrade(isBanned = false)

            presenter.onViewAttached()
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertTrue(state.isTradeLoaded)
            assertEquals(ACCOUNT_DATA, state.paymentAccountData)
            assertTrue(state.isConfirmFiatSentEnabled)
            assertFalse(state.isAccountDataBanned)
            assertFalse(state.isBannedWarningVisible)
            coVerify(exactly = 0) { userProfileServiceFacade.reportUserProfile(any(), any()) }
            coVerify(exactly = 0) { tradesServiceFacade.cancelTradeForBannedAccountData() }
        }

    @Test
    fun `no selected trade leaves the trade unloaded`() =
        runTest {
            val presenter = givenTrade()
            selectedTrade.value = null

            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isTradeLoaded)
        }

    @Test
    fun `missing account data keeps confirm disabled and skips the check`() =
        runTest {
            paymentAccountData.value = null
            val presenter = givenTrade()

            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isConfirmFiatSentEnabled)
            coVerify(exactly = 0) { tradesServiceFacade.isAccountDataBanned(any()) }
        }

    @Test
    fun `banned account data blocks confirm, shows warning and reports once across re-attach`() =
        runTest {
            val presenter = givenTrade(isBanned = true)

            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onViewUnattaching()
            advanceUntilIdle()
            presenter.onViewAttached()
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertTrue(state.isAccountDataBanned)
            assertTrue(state.isBannedWarningVisible)
            assertFalse(state.isConfirmFiatSentEnabled)
            coVerify(exactly = 1) {
                userProfileServiceFacade.reportUserProfile(peer, "Account data of seller is banned: $ACCOUNT_DATA")
            }
            coVerify(exactly = 0) { tradesServiceFacade.cancelTradeForBannedAccountData() }
        }

    @Test
    fun `a second banned trade on the same presenter reports its seller too`() =
        runTest {
            val presenter = givenTrade(isBanned = true)
            coEvery { tradesServiceFacade.isAccountDataBanned(OTHER_ACCOUNT_DATA) } returns true
            presenter.onViewAttached()
            advanceUntilIdle()

            selectedTrade.value = trade(MutableStateFlow(OTHER_ACCOUNT_DATA), id = "trade-2")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                userProfileServiceFacade.reportUserProfile(peer, "Account data of seller is banned: $OTHER_ACCOUNT_DATA")
            }
        }

    @Test
    fun `acknowledging the warning hides it and cancels the trade, confirm stays disabled`() =
        runTest {
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertFalse(state.isBannedWarningVisible)
            assertFalse(state.isConfirmFiatSentEnabled)
            coVerify(exactly = 1) { tradesServiceFacade.cancelTradeForBannedAccountData() }
        }

    @Test
    fun `failed report still lets the cancel run on acknowledge`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.failure(RuntimeException("offline"))
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.cancelTradeForBannedAccountData() }
        }

    @Test
    fun `acknowledging the warning twice cancels the trade once`() =
        runTest {
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.cancelTradeForBannedAccountData() }
        }

    @Test
    fun `acknowledging again after a re-attach does not start a second cancel`() =
        runTest {
            coEvery { tradesServiceFacade.cancelTradeForBannedAccountData() } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            advanceUntilIdle()

            presenter.onViewUnattaching()
            advanceUntilIdle()
            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.cancelTradeForBannedAccountData() }
        }

    @Test
    fun `switching to a trade with clean account data hides the warning`() =
        runTest {
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()

            selectedTrade.value = trade(MutableStateFlow(OTHER_ACCOUNT_DATA))
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertFalse(state.isAccountDataBanned)
            assertFalse(state.isBannedWarningVisible)
        }

    @Test
    fun `switching trades keeps confirm disabled until the new check resolves`() =
        runTest {
            val presenter = givenTrade()
            val check = CompletableDeferred<Boolean>()
            coEvery { tradesServiceFacade.isAccountDataBanned(OTHER_ACCOUNT_DATA) } coAnswers { check.await() }
            presenter.onViewAttached()
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isConfirmFiatSentEnabled)

            selectedTrade.value = trade(MutableStateFlow(OTHER_ACCOUNT_DATA))
            advanceUntilIdle()
            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isTradeLoaded)
            assertFalse(presenter.uiState.value.isConfirmFiatSentEnabled)
            coVerify(exactly = 0) { tradesServiceFacade.buyerConfirmFiatSent() }

            check.complete(false)
            advanceUntilIdle()

            assertEquals(OTHER_ACCOUNT_DATA, presenter.uiState.value.paymentAccountData)
            assertTrue(presenter.uiState.value.isConfirmFiatSentEnabled)
        }

    @Test
    fun `a failing banned check keeps confirm disabled and is retried until it answers`() =
        runTest {
            val presenter = givenTrade()
            coEvery { tradesServiceFacade.isAccountDataBanned(ACCOUNT_DATA) } throws
                RuntimeException("failed") andThenThrows RuntimeException("failed") andThen false

            presenter.onViewAttached()
            runCurrent()
            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            runCurrent()

            assertFalse(presenter.uiState.value.isTradeLoaded)
            assertFalse(presenter.uiState.value.isConfirmFiatSentEnabled)
            coVerify(exactly = 0) { tradesServiceFacade.buyerConfirmFiatSent() }

            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isTradeLoaded)
            assertTrue(presenter.uiState.value.isConfirmFiatSentEnabled)
        }

    @Test
    fun `detaching resets the state`() =
        runTest {
            val presenter = givenTrade()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isConfirmFiatSentEnabled)

            presenter.onViewUnattaching()

            assertEquals(BuyerState2aUiState(), presenter.uiState.value)
        }

    @Test
    fun `a trade opened after a confirmed one can be confirmed`() =
        runTest {
            val presenter = givenTrade()
            coEvery { tradesServiceFacade.isAccountDataBanned(OTHER_ACCOUNT_DATA) } returns false
            coEvery { tradesServiceFacade.buyerConfirmFiatSent() } returns Result.success(Unit)
            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            advanceUntilIdle()
            coVerify(exactly = 1) { tradesServiceFacade.buyerConfirmFiatSent() }
            presenter.onViewUnattaching()
            advanceUntilIdle()

            selectedTrade.value = trade(MutableStateFlow(OTHER_ACCOUNT_DATA), id = "trade-2")
            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isConfirmFiatSentEnabled)
        }

    @Test
    fun `failed cancel shows an error`() =
        runTest {
            coEvery { tradesServiceFacade.cancelTradeForBannedAccountData() } returns
                Result.failure(RuntimeException("failed"))
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning)
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar(any(), SnackbarType.ERROR, any(), any()) }
        }

    @Test
    fun `confirm while banned does not send the fiat confirmation`() =
        runTest {
            val presenter = givenTrade(isBanned = true)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            advanceUntilIdle()

            coVerify(exactly = 0) { tradesServiceFacade.buyerConfirmFiatSent() }
        }

    @Test
    fun `rapid double-tap on confirm triggers buyerConfirmFiatSent only once`() =
        runTest {
            val presenter = givenTrade()
            coEvery { tradesServiceFacade.buyerConfirmFiatSent() } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.buyerConfirmFiatSent() }
            assertFalse(presenter.uiState.value.isConfirmFiatSentEnabled)
        }

    @Test
    fun `confirm fiat sent failure re-enables confirm`() =
        runTest {
            val presenter = givenTrade()
            coEvery { tradesServiceFacade.buyerConfirmFiatSent() } returns
                Result.failure(RuntimeException("failed"))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(BuyerState2aUiAction.OnConfirmFiatSent)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isConfirmFiatSentEnabled)
        }

    private companion object {
        const val ACCOUNT_DATA = "IBAN: DE89370400440532013000"
        const val OTHER_ACCOUNT_DATA = "IBAN: DE12500105170648489890"
    }
}
