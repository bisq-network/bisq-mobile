package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_a

import android.view.Window
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.clipboardPrimaryText
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import network.bisq.mobile.test.presentation.compose.CaptureHostWindow
import network.bisq.mobile.test.presentation.compose.isSecure
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SellerState3aUiTest : BisqComposeUiTestBase() {
    @Test
    fun `buyer bitcoin address view blocks screenshots even before the trade loads`() {
        val presenter =
            mockk<SellerState3aPresenter>(relaxed = true) {
                every { selectedTrade } returns MutableStateFlow(null)
            }
        lateinit var window: Window

        setTestContent {
            CaptureHostWindow { window = it }
            SellerState3a(presenter = presenter)
        }

        assertTrue(window.isSecure)
    }

    @Test
    fun `seller can copy both the amount and the buyer address`() {
        setTestContent {
            SellerState3a(presenter = presenterWithTrade(bitcoinPaymentData = BUYER_ADDRESS))
        }

        val copyButtons = composeTestRule.onAllNodesWithContentDescription(COPY)
        copyButtons.assertCountEquals(2)

        copyButtons[0].performClick()
        composeTestRule.waitForIdle()
        assertEquals(BASE_AMOUNT, clipboardPrimaryText())

        // A copied button shows a check mark for a moment; wait until both read "copy" again.
        composeTestRule.mainClock.advanceTimeBy(2_500)
        composeTestRule.waitForIdle()

        copyButtons[1].performClick()
        composeTestRule.waitForIdle()
        assertEquals(BUYER_ADDRESS, clipboardPrimaryText())
    }

    private fun presenterWithTrade(bitcoinPaymentData: String?): SellerState3aPresenter {
        val trade =
            mockk<TradeItemPresentationModel>(relaxed = true) {
                every { formattedBaseAmount } returns BASE_AMOUNT
                every { quoteAmountWithCode } returns "100.00 USD"
                every { bisqEasyTradeModel.bitcoinPaymentData } returns MutableStateFlow(bitcoinPaymentData)
            }
        return mockk(relaxed = true) {
            every { selectedTrade } returns MutableStateFlow(trade)
            every { paymentProof } returns MutableStateFlow(null)
            every { buttonEnabled } returns MutableStateFlow(true)
            every { isConfirmSendEnabled } returns MutableStateFlow(false)
            every { isLightning } returns MutableStateFlow(false)
            every { bitcoinLnAddressFieldType } returns MutableStateFlow(BitcoinLnAddressFieldType.Bitcoin)
        }
    }

    private companion object {
        const val BUYER_ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        const val BASE_AMOUNT = "0.00100000"
        const val COPY = "Copy icon"
    }
}
