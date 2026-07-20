package network.bisq.mobile.client.network.presentation.network

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeUiTestBase
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class ClientNetworkOverviewContentTest : BisqComposeUiTestBase() {
    @Test
    fun `when reachable then topology, routing, peer count and health render`() {
        setTestContent {
            ClientNetworkOverviewContent(
                uiState =
                    ClientNetworkOverviewUiState(
                        trustedNodeHost = "r7m2xpqowg3bvf8t.onion",
                        isReachable = true,
                        isTorRouted = true,
                        peerCountViaNode = 12,
                        healthState = NetworkHealthState.HEALTHY,
                    ),
                onAction = {},
                topBar = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.networkInfo.overview.health.healthy".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.networkInfo.connect.topology.connectedVia".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.networkInfo.connect.routing.tor".i18n()).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.networkInfo.overview.connections".i18nPlural(12)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.networkInfo.connect.myConnection.title".i18n()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `when unreachable then offline health, not-available peers and check-settings render`() {
        setTestContent {
            ClientNetworkOverviewContent(
                uiState = ClientNetworkOverviewUiState(trustedNodeHost = null),
                onAction = {},
                topBar = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.networkInfo.overview.health.offline".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.networkInfo.connect.checkSettings".i18n()).performScrollTo().assertIsDisplayed()
        // Offline ⇒ both the latency row and the peer-count row render "Not available".
        composeTestRule.onAllNodesWithText("mobile.networkInfo.connect.notAvailable".i18n()).assertCountEquals(2)
    }
}
