package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import android.view.Window
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import network.bisq.mobile.test.presentation.compose.CaptureHostWindow
import network.bisq.mobile.test.presentation.compose.isSecure
import org.junit.Test
import kotlin.test.assertTrue

class BuyerState1aUiTest : BisqComposeUiTestBase() {
    @Test
    fun `bitcoin address entry blocks screenshots`() {
        val presenter =
            mockk<BuyerState1aPresenter>(relaxed = true) {
                every { uiState } returns MutableStateFlow(BuyerState1aUiState())
                every { isSendBitcoinPaymentDataEnabled } returns MutableStateFlow(true)
            }
        lateinit var window: Window

        setTestContent {
            CaptureHostWindow { window = it }
            BuyerState1a(presenter = presenter)
        }

        assertTrue(window.isSecure)
    }
}
