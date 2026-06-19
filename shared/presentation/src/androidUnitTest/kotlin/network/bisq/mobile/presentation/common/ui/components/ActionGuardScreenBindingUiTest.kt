package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1a
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2a
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a.BuyerState2aPresenter
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class ActionGuardScreenBindingUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

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
        val trade = mockTrade()
        val presenter = mockk<BuyerState2aPresenter>(relaxed = true)
        every { presenter.selectedTrade } returns MutableStateFlow(trade)
        every { presenter.isConfirmFiatSentEnabled } returns MutableStateFlow(false)

        setTestContent {
            BuyerState2a(presenter = presenter)
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent".i18n(trade.quoteAmountWithCode))
            .assertIsNotEnabled()
    }

    @Test
    fun `BuyerState1a disables send button when guard is false`() {
        val presenter = mockk<BuyerState1aPresenter>(relaxed = true)
        every { presenter.headline } returns MutableStateFlow("Headline")
        every { presenter.description } returns MutableStateFlow("Description")
        every { presenter.bitcoinPaymentData } returns MutableStateFlow("bc1qtest")
        every { presenter.bitcoinLnAddressFieldType } returns MutableStateFlow(mockk(relaxed = true))
        every { presenter.triggerBitcoinLnAddressValidation } returns MutableStateFlow(0)
        every { presenter.isSendBitcoinPaymentDataEnabled } returns MutableStateFlow(false)

        setTestContent {
            BuyerState1a(presenter = presenter)
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.buyer.phase1a.send".i18n())
            .assertIsNotEnabled()
    }

    private fun mockTrade(): TradeItemPresentationModel {
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.quoteAmountWithCode } returns "100 USD"
        every { trade.bisqEasyTradeModel.paymentAccountData } returns MutableStateFlow("account")
        every { trade.bisqEasyTradeModel.shortId } returns "abc123"
        return trade
    }
}
