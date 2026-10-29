package network.bisq.mobile.presentation.common.ui.components.organisms.trades

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class OtherAmountWarningDialogUiTest : BisqComposeUiTestBase() {
    @Test
    fun `shows the trade amount and closes without a proceed option`() {
        val onDismiss = mockk<() -> Unit>(relaxed = true)

        setTestContent { OtherAmountWarningDialog(tradeAmountWithCode = "0.0025 BTC", onDismiss = onDismiss) }

        composeTestRule
            .onNodeWithText("mobile.tradeState.buyer.phase1a.otherAmount.warning".i18n("0.0025 BTC"))
            .assertExists()
        composeTestRule.onNodeWithText("bisqEasy.takeOffer.bitcoinPaymentData.warning.proceed".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText("action.close".i18n()).performClick()

        verify(exactly = 1) { onDismiss() }
    }
}
