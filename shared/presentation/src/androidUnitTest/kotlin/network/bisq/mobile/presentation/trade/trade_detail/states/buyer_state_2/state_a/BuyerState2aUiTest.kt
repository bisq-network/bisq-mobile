package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

import android.view.Window
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import network.bisq.mobile.test.presentation.compose.CaptureHostWindow
import network.bisq.mobile.test.presentation.compose.isSecure
import org.junit.Test
import kotlin.test.assertTrue

class BuyerState2aUiTest : BisqComposeUiTestBase() {
    private val loadedState =
        BuyerState2aUiState(
            isTradeLoaded = true,
            quoteAmountWithCode = "100 USD",
            paymentAccountData = "IBAN: DE89370400440532013000",
            tradeShortId = "abc123",
            isConfirmFiatSentEnabled = true,
        )
    private val bannedState = loadedState.copy(isConfirmFiatSentEnabled = false, isAccountDataBanned = true)
    private val confirmButtonText get() = "bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent".i18n("100 USD")
    private val cancelTradeText get() = "bisqEasy.openTrades.cancelTrade".i18n()
    private val reasonForPaymentText get() = "mobile.tradeState.info.buyer.phase2a.reasonForPaymentInfo".i18n("abc123")

    private fun presenterWith(state: BuyerState2aUiState) =
        mockk<BuyerState2aPresenter>(relaxed = true) {
            every { uiState } returns MutableStateFlow(state)
        }

    @Test
    fun `seller account data view blocks screenshots even before the trade loads`() {
        val presenter = presenterWith(BuyerState2aUiState())
        lateinit var window: Window

        setTestContent {
            CaptureHostWindow { window = it }
            BuyerState2a(presenter = presenter)
        }

        assertTrue(window.isSecure)
    }

    @Test
    fun `seller account data on screen blocks screenshots`() {
        val presenter = presenterWith(loadedState)
        lateinit var window: Window

        setTestContent {
            CaptureHostWindow { window = it }
            BuyerState2a(presenter = presenter)
        }

        assertTrue(window.isSecure)
    }

    @Test
    fun `account data not banned shows the reason for payment hint without banner or warning`() {
        setTestContent { BuyerState2a(presenter = presenterWith(loadedState)) }

        composeTestRule.onNodeWithText(reasonForPaymentText).assertExists()
        composeTestRule.onNodeWithTag(BANNED_ACCOUNT_BANNER_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).assertDoesNotExist()
    }

    @Test
    fun `banned account data shows the banner instead of the hint and disables confirm`() {
        setTestContent { BuyerState2a(presenter = presenterWith(bannedState)) }

        composeTestRule.onNodeWithTag(BANNED_ACCOUNT_BANNER_TAG).assertExists()
        composeTestRule
            .onNodeWithText("mobile.tradeState.info.buyer.phase2a.accountDataBanned.banner.action".i18n())
            .assertExists()
        composeTestRule.onNodeWithText(reasonForPaymentText).assertDoesNotExist()
        composeTestRule.onNodeWithText(confirmButtonText).assertIsNotEnabled()
        // Acknowledged: the banner stays without the warning dialog.
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(cancelTradeText).assertDoesNotExist()
    }

    @Test
    fun `a failed cancel shows the failure in the banner with a cancel button`() {
        setTestContent { BuyerState2a(presenter = presenterWith(bannedState.copy(isBannedCancelFailed = true))) }

        composeTestRule
            .onNodeWithText("mobile.tradeState.info.buyer.phase2a.accountDataBanned.banner.cancelFailed".i18n())
            .assertExists()
        composeTestRule
            .onNodeWithText("mobile.tradeState.info.buyer.phase2a.accountDataBanned.banner.action".i18n())
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(cancelTradeText).assertExists()
        composeTestRule.onNodeWithText(confirmButtonText).assertIsNotEnabled()
    }

    @Test
    fun `the cancel button of a failed cancel dispatches the retry action`() {
        val presenter = presenterWith(bannedState.copy(isBannedCancelFailed = true))

        setTestContent { BuyerState2a(presenter = presenter) }
        composeTestRule.onNodeWithText(cancelTradeText).performClick()

        verify { presenter.onAction(BuyerState2aUiAction.OnRetryBannedCancel) }
    }

    @Test
    fun `the banned warning says the trade is cancelled on closing it`() {
        setTestContent { BuyerState2a(presenter = presenterWith(bannedState.copy(isBannedWarningVisible = true))) }

        composeTestRule
            .onNodeWithText("mobile.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning".i18n())
            .assertExists()
    }

    @Test
    fun `acknowledging the banned warning dispatches the acknowledge action`() {
        val presenter = presenterWith(bannedState.copy(isBannedWarningVisible = true))

        setTestContent { BuyerState2a(presenter = presenter) }
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        verify { presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning) }
    }

    @Test
    fun `back on the banned warning dispatches the acknowledge action`() {
        val presenter = presenterWith(bannedState.copy(isBannedWarningVisible = true))

        setTestContent { BuyerState2a(presenter = presenter) }
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).assertExists()
        pressBack()
        composeTestRule.waitForIdle()

        verify { presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning) }
    }
}
