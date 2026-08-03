package network.bisq.mobile.presentation.common.ui.components.network

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [ConnectionCard], the per-peer card shared by the Node and Connect apps.
 *
 * Scoped to what only the component can express — address truncation, RTT formatting, and the
 * expand/collapse interaction. The screen-level tests in each app cover how the card is composed into
 * a list.
 */
class ConnectionCardUiTest : BisqComposeUiTestBase() {
    private val longAddress = "wns3jrgvyxjafp7sazk4nzgcpndinftwotgxu4tdpg6ov5xshhbc4qd.onion:1893"
    private val shortAddress = "abcd.onion:1234"

    @Test
    fun `when the address is too long then it is middle-truncated keeping the port`() {
        setTestContent { ConnectionCard(peer = peer(address = longAddress)) }

        // Head + ellipsis + tail: plain tail-ellipsis would discard the ":1893" that distinguishes
        // a user's own nodes from one another.
        composeTestRule.onNodeWithText("wns3jrgvyx…onion:1893").assertIsDisplayed()
    }

    @Test
    fun `when the address fits then it is displayed in full`() {
        setTestContent { ConnectionCard(peer = peer(address = shortAddress)) }

        composeTestRule.onNodeWithText(shortAddress).assertIsDisplayed()
    }

    @Test
    fun `when the rtt is under a second then it is rendered in milliseconds`() {
        setTestContent { ConnectionCard(peer = peer(metrics = metrics(rttMillis = 184L))) }

        composeTestRule.onNodeWithText("184 ms").assertIsDisplayed()
    }

    @Test
    fun `when the rtt is a second or more then it is rendered in seconds`() {
        setTestContent { ConnectionCard(peer = peer(metrics = metrics(rttMillis = 1_240L))) }

        composeTestRule.onNodeWithText("1.2 s").assertIsDisplayed()
    }

    @Test
    fun `when the rtt is not yet measured then a dash is rendered`() {
        setTestContent { ConnectionCard(peer = peer(metrics = metrics(rttMillis = null))) }

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun `when a card is tapped twice then it expands and collapses again`() {
        setTestContent { ConnectionCard(peer = peer(metrics = metrics())) }

        // The chevron is one rotated asset, so its direction is not observable in a unit test — the
        // localized affordance description is. Asserting it here is also what makes the "no chevron
        // when a peer has no metrics" test below meaningful rather than vacuously green.
        composeTestRule.onNodeWithContentDescription(showLabel).assertIsDisplayed()

        composeTestRule.onNodeWithTag("connection_card_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(sentLabel).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(hideLabel).assertIsDisplayed()

        composeTestRule.onNodeWithTag("connection_card_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(sentLabel).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(showLabel).assertIsDisplayed()
    }

    @Test
    fun `when initiallyExpanded is set then the traffic section renders without a tap`() {
        setTestContent {
            ConnectionCard(peer = peer(metrics = metrics()), initiallyExpanded = true)
        }

        composeTestRule.onNodeWithText(sentLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(receivedLabel).assertIsDisplayed()
    }

    @Test
    fun `when the copy icon is tapped then the card does not expand`() {
        // The copy icon sits inside the card's own clickable area; Compose's nested-clickable
        // consumption must stop the tap before it reaches the card's expand handler.
        setTestContent { ConnectionCard(peer = peer(metrics = metrics())) }

        composeTestRule.onNodeWithContentDescription("Copy icon").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(sentLabel).assertDoesNotExist()
    }

    @Test
    fun `when a peer has no metrics then neither the rtt nor the chevron renders`() {
        setTestContent { ConnectionCard(peer = peer(metrics = null)) }

        composeTestRule.onNodeWithText("–").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(showLabel).assertDoesNotExist()
    }

    @Test
    fun `when a peer is not a seed then no seed badge renders`() {
        setTestContent { ConnectionCard(peer = peer(isSeed = false)) }

        composeTestRule.onNodeWithText("mobile.networkInfo.connections.seed".i18n()).assertDoesNotExist()
    }

    private val sentLabel get() = "mobile.networkInfo.connections.sent".i18n()
    private val receivedLabel get() = "mobile.networkInfo.connections.received".i18n()
    private val showLabel get() = "mobile.action.show".i18n()
    private val hideLabel get() = "mobile.action.hide".i18n()

    private fun peer(
        address: String = shortAddress,
        isSeed: Boolean = false,
        metrics: ConnectionMetricsUiItem? = null,
    ): NetworkConnectionUiItem =
        NetworkConnectionUiItem(
            connectionId = "1",
            address = address,
            isOutbound = true,
            isSeed = isSeed,
            establishedAtMillis = 0L,
            metrics = metrics,
        )

    private fun metrics(rttMillis: Long? = 184L): ConnectionMetricsUiItem =
        ConnectionMetricsUiItem(
            rttMillis = rttMillis,
            sentBytes = 12_400L,
            sentMessageCount = 340L,
            receivedBytes = 18_900L,
            receivedMessageCount = 512L,
        )
}
