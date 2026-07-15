package network.bisq.mobile.node.network.presentation.network

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.node.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class NetworkContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        uiState: NetworkUiState,
        onAction: (NetworkUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    NetworkContent(
                        uiState = uiState,
                        onAction = onAction,
                        topBar = {},
                    )
                }
            }
        }
    }

    @Test
    fun `when healthy state then healthy label is displayed`() {
        setTestContent(uiState = sampleUiState(healthState = NetworkHealthState.HEALTHY))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.health.healthy".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when offline state then offline label is displayed`() {
        setTestContent(uiState = sampleUiState(healthState = NetworkHealthState.OFFLINE))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.health.offline".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when peers connected then peer count is displayed`() {
        setTestContent(uiState = sampleUiState(peerCount = 7))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("7").assertIsDisplayed()
    }

    @Test
    fun `when tor is running then running value is displayed`() {
        setTestContent(uiState = sampleUiState(isTorRunning = true))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.torRunning".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when tor is stopped then stopped value is displayed`() {
        setTestContent(uiState = sampleUiState(isTorRunning = false))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.torStopped".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when connections subtitle then it shows the connected peer count`() {
        setTestContent(uiState = sampleUiState(peerCount = 7))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.connections".i18nPlural(7))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when there is a single peer then the connections subtitle uses singular grammar`() {
        setTestContent(uiState = sampleUiState(peerCount = 1))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.connections".i18nPlural(1))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when onion address is present then it is displayed`() {
        val address = "jd4tx3nljykg5z3vqy7w2m8n4p6r0t2u4w6y8a0c2e4g6i8k.onion:1234"
        setTestContent(uiState = sampleUiState(onionAddress = address))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(address)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when onion address is null then the loading label is displayed`() {
        setTestContent(uiState = sampleUiState(onionAddress = null))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.overview.addressLoading".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when connections card is clicked then OnConnectionsClick is dispatched`() {
        var captured: NetworkUiAction? = null
        setTestContent(uiState = sampleUiState(), onAction = { captured = it })
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.connections.title".i18n())
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(NetworkUiAction.OnConnectionsClick, captured)
    }

    @Test
    fun `when my node card is clicked then OnMyNodeClick is dispatched`() {
        var captured: NetworkUiAction? = null
        setTestContent(uiState = sampleUiState(), onAction = { captured = it })
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.networkInfo.myNode.title".i18n())
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(NetworkUiAction.OnMyNodeClick, captured)
    }

    private fun sampleUiState(
        peerCount: Int = 7,
        isTorRunning: Boolean = true,
        onionAddress: String? = "jd4tx3nljykg5z3vqy7w2m8n4p6r0t2u4w6y8a0c2e4g6i8k.onion:1234",
        healthState: NetworkHealthState = NetworkHealthState.HEALTHY,
    ): NetworkUiState =
        NetworkUiState(
            peerCount = peerCount,
            isTorRunning = isTorRunning,
            onionAddress = onionAddress,
            healthState = healthState,
        )
}
