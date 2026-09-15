package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * The pre-fill treatment on the buyer's address screen: badge and confirmation checkbox appear
 * only for a pre-filled value, and Send stays gated until the checkbox lifts it. The gate's
 * state machine itself (seeding, edit lifting, clearing on send) lives in
 * [BuyerState1aPresenterTest]; this covers the screen wiring.
 */
class BuyerState1aPrefillUiTest : BisqComposeUiTestBase() {
    private val badgeText get() = "mobile.tradeState.buyer.phase1a.prefilled.badge".i18n()
    private val checkboxText get() = "mobile.tradeState.buyer.phase1a.prefilled.confirmCheckbox".i18n()
    private val sendText get() = "bisqEasy.tradeState.info.buyer.phase1a.send".i18n()

    private fun presenterWith(
        wasPrefilled: Boolean,
        hasConfirmedPrefill: Boolean = false,
    ): BuyerState1aPresenter =
        mockk<BuyerState1aPresenter>(relaxed = true) {
            every { headline } returns MutableStateFlow("Fill in your Bitcoin address")
            every { description } returns MutableStateFlow("Bitcoin address")
            every { bitcoinPaymentData } returns MutableStateFlow("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")
            every { bitcoinLnAddressFieldType } returns MutableStateFlow(BitcoinLnAddressFieldType.Bitcoin)
            every { triggerBitcoinLnAddressValidation } returns MutableStateFlow(0)
            every { isSendBitcoinPaymentDataEnabled } returns MutableStateFlow(true)
            every { this@mockk.wasPrefilled } returns MutableStateFlow(wasPrefilled)
            every { this@mockk.hasConfirmedPrefill } returns MutableStateFlow(hasConfirmedPrefill)
        }

    @Test
    fun `prefilled and unconfirmed shows badge and checkbox and disables send`() {
        val presenter = presenterWith(wasPrefilled = true)

        setTestContent { BuyerState1a(presenter = presenter) }

        composeTestRule.onNodeWithText(badgeText).assertExists()
        composeTestRule.onNodeWithText(checkboxText).assertExists()
        composeTestRule.onNodeWithText(sendText).assertIsNotEnabled()
    }

    @Test
    fun `checking the confirmation enables send and reaches the presenter`() {
        val presenter = presenterWith(wasPrefilled = true, hasConfirmedPrefill = true)

        setTestContent { BuyerState1a(presenter = presenter) }

        composeTestRule.onNodeWithText(sendText).assertIsEnabled().performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { presenter.onSendBitcoinPaymentDataClick() }
    }

    @Test
    fun `checkbox tap reports the confirmation change`() {
        val presenter = presenterWith(wasPrefilled = true)

        setTestContent { BuyerState1a(presenter = presenter) }

        composeTestRule.onNodeWithText(checkboxText).performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { presenter.onConfirmPrefillChange(true) }
    }

    @Test
    fun `without a prefill the screen renders exactly as before`() {
        val presenter = presenterWith(wasPrefilled = false)

        setTestContent { BuyerState1a(presenter = presenter) }

        composeTestRule.onNodeWithText(badgeText).assertDoesNotExist()
        composeTestRule.onNodeWithText(checkboxText).assertDoesNotExist()
        composeTestRule.onNodeWithText(sendText).assertIsEnabled()
    }
}
