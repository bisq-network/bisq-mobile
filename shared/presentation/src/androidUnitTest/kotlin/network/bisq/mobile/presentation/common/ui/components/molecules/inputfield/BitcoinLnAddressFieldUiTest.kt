package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.clipboardPrimaryText
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals

/**
 * The same field serves two roles: the buyer enters their payout address or invoice (paste and
 * scan), and the seller reads it back to pay it (copy). A read-only field must always offer copy,
 * because a disabled text field cannot be selected and a Lightning invoice cannot be retyped.
 */
class BitcoinLnAddressFieldUiTest : BisqComposeUiTestBase() {
    @Test
    fun `read-only address offers copy and copies the address`() {
        setTestContent {
            BitcoinLnAddressField(value = ADDRESS, disabled = true)
        }

        composeTestRule.onNodeWithContentDescription(COPY).performClick()
        composeTestRule.waitForIdle()

        assertEquals(ADDRESS, clipboardPrimaryText())
    }

    @Test
    fun `read-only lightning invoice offers copy and copies the invoice`() {
        setTestContent {
            BitcoinLnAddressField(
                value = INVOICE,
                disabled = true,
                type = BitcoinLnAddressFieldType.Lightning,
            )
        }

        composeTestRule.onNodeWithContentDescription(COPY).performClick()
        composeTestRule.waitForIdle()

        assertEquals(INVOICE, clipboardPrimaryText())
    }

    @Test
    fun `read-only field offers neither paste nor scan`() {
        setTestContent {
            BitcoinLnAddressField(value = ADDRESS, disabled = true)
        }

        composeTestRule.onNodeWithContentDescription(PASTE).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(SCAN).assertDoesNotExist()
    }

    @Test
    fun `read-only field without a value offers no copy`() {
        setTestContent {
            BitcoinLnAddressField(value = "", disabled = true)
        }

        composeTestRule.onNodeWithContentDescription(COPY).assertDoesNotExist()
    }

    @Test
    fun `read-only placeholder that is not payment data offers no copy`() {
        setTestContent {
            BitcoinLnAddressField(value = "N/A", disabled = true, copyable = false)
        }

        composeTestRule.onNodeWithContentDescription(COPY).assertDoesNotExist()
    }

    @Test
    fun `editable field offers paste and scan but not copy`() {
        setTestContent {
            BitcoinLnAddressField(value = ADDRESS, onBarcodeClick = {})
        }

        composeTestRule.onNodeWithContentDescription(PASTE).assertExists()
        composeTestRule.onNodeWithContentDescription(SCAN).assertExists()
        composeTestRule.onNodeWithContentDescription(COPY).assertDoesNotExist()
    }

    private companion object {
        const val ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        const val INVOICE = "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq"
        const val COPY = "Copy icon"
        const val PASTE = "Paste icon"
        const val SCAN = "Scan icon"
    }
}
