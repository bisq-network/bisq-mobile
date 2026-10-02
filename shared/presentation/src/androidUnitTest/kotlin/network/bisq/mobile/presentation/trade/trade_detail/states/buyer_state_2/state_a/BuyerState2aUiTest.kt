package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

import android.view.Window
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `account data not banned shows the reason for payment hint, no banner and no warning`() {
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
    }

    @Test
    fun `acknowledging the banned warning dispatches the acknowledge action`() {
        val presenter = presenterWith(bannedState.copy(isBannedWarningVisible = true))

        setTestContent { BuyerState2a(presenter = presenter) }
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        verify { presenter.onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning) }
    }
}
