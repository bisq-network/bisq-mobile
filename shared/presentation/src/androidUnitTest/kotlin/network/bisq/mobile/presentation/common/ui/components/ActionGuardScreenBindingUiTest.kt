package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1a
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aUiState
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2a
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2aUiState
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertFalse

class ActionGuardScreenBindingUiTest : BisqComposeUiTestBase() {
    @Test
    fun `ChatInputField respects sendEnabled false`() {
        var sent = false
        val placeholder = "Type message"
        setTestContent {
            ChatInputField(
                onMessageSend = { sent = true },
                placeholder = placeholder,
                sendEnabled = false,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(placeholder)
            .performTextInput("hello")
        composeTestRule
            .onNodeWithContentDescription("Send icon")
            .assertIsNotEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(sent)
    }

    @Test
    fun `BuyerState2a disables confirm button when guard is false`() {
        val presenter = mockk<BuyerState2aPresenter>(relaxed = true)
        every { presenter.uiState } returns
            MutableStateFlow(
                BuyerState2aUiState(
                    isTradeLoaded = true,
                    quoteAmountWithCode = "100 USD",
                    paymentAccountData = "account",
                    tradeShortId = "abc123",
                    isConfirmFiatSentEnabled = false,
                ),
            )

        setTestContent {
            BuyerState2a(presenter = presenter)
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent".i18n("100 USD"))
            .assertIsNotEnabled()
    }

    @Test
    fun `BuyerState1a disables send button when guard is false`() {
        val presenter = mockk<BuyerState1aPresenter>(relaxed = true)
        every { presenter.uiState } returns
            MutableStateFlow(BuyerState1aUiState(headline = "Headline", description = "Description", paymentData = "bc1qtest"))
        every { presenter.isSendBitcoinPaymentDataEnabled } returns MutableStateFlow(false)

        setTestContent {
            BuyerState1a(presenter = presenter)
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.buyer.phase1a.send".i18n())
            .assertIsNotEnabled()
    }
}
