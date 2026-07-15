package network.bisq.mobile.node.network.presentation.connections

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.node.common.domain.service.network.NodePeerInfo
import network.bisq.mobile.node.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class NetworkConnectionsContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(uiState: NetworkConnectionsUiState) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    NetworkConnectionsContent(uiState = uiState, topBar = {})
                }
            }
        }
    }

    @Test
    fun `when there are no peers then the empty state is displayed`() {
        setTestContent(NetworkConnectionsUiState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.empty".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.emptyHint".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when peers are present then the peer count line is displayed`() {
        setTestContent(sampleState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.peers".i18nPlural(2))
            .assertIsDisplayed()
    }

    @Test
    fun `when there is a single peer then the peer count line uses singular grammar`() {
        setTestContent(singlePeerState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.peers".i18nPlural(1))
            .assertIsDisplayed()
    }

    @Test
    fun `when peers are present then their addresses are displayed`() {
        setTestContent(sampleState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("outbound.onion:1234")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when a peer is outbound then the outbound label is displayed`() {
        setTestContent(sampleState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.outbound".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when a peer is inbound then the inbound label is displayed`() {
        setTestContent(sampleState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.inbound".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when a peer is a seed then the seed badge is displayed`() {
        setTestContent(sampleState())
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.seed".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun sampleState(): NetworkConnectionsUiState =
        NetworkConnectionsUiState(
            peerCount = 2,
            peers =
                listOf(
                    NodePeerInfo(
                        connectionId = "1",
                        address = "outbound.onion:1234",
                        isOutbound = true,
                        establishedAtMillis = 0L,
                        isSeed = true,
                    ),
                    NodePeerInfo(
                        connectionId = "2",
                        address = "inbound.onion:1234",
                        isOutbound = false,
                        establishedAtMillis = 0L,
                        isSeed = false,
                    ),
                ),
        )

    private fun singlePeerState(): NetworkConnectionsUiState =
        NetworkConnectionsUiState(
            peerCount = 1,
            peers =
                listOf(
                    NodePeerInfo(
                        connectionId = "1",
                        address = "outbound.onion:1234",
                        isOutbound = true,
                        establishedAtMillis = 0L,
                        isSeed = false,
                    ),
                ),
        )
}
