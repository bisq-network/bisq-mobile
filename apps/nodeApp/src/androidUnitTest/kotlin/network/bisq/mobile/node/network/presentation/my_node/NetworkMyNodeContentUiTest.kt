package network.bisq.mobile.node.network.presentation.my_node

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.test_utils.TestApplication
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class NetworkMyNodeContentUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when node info is present then the onion address and key id are displayed`() {
        setTestContent {
            NetworkMyNodeContent(uiState = sampleState(), topBar = {})
        }

        composeTestRule
            .onNodeWithText("mynode.onion:1234")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("135e9801eb1b50d29e6d0035e93")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when node info is present then the app version is displayed`() {
        setTestContent {
            NetworkMyNodeContent(uiState = sampleState(), topBar = {})
        }

        composeTestRule
            .onNodeWithText("0.4.2")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when tor is running then the running status is displayed`() {
        setTestContent {
            NetworkMyNodeContent(uiState = sampleState(), topBar = {})
        }

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.torRunning".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when tor is not running then the stopped status is displayed`() {
        setTestContent {
            NetworkMyNodeContent(uiState = sampleState().copy(isTorRunning = false), topBar = {})
        }

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.torStopped".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when node info is not yet resolved then the loading fallback is displayed`() {
        // Both onion address and keyId fall back to the loading text.
        setTestContent {
            NetworkMyNodeContent(uiState = NetworkMyNodeUiState(appVersion = "0.4.2"), topBar = {})
        }

        composeTestRule
            .onAllNodesWithText("mobile.networkInfo.overview.addressLoading".i18n())
            .onFirst()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when populated then the field labels are displayed`() {
        setTestContent {
            NetworkMyNodeContent(uiState = sampleState(), topBar = {})
        }

        composeTestRule
            .onNodeWithText("mobile.networkInfo.myNode.onionAddress".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.networkInfo.myNode.keyId".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun sampleState(): NetworkMyNodeUiState =
        NetworkMyNodeUiState(
            onionAddress = "mynode.onion:1234",
            keyId = "135e9801eb1b50d29e6d0035e93",
            appVersion = "0.4.2",
            isTorRunning = true,
        )
}
