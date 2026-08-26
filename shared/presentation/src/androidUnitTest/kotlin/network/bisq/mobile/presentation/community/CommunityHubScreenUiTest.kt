package network.bisq.mobile.presentation.community

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals

/**
 * UI tests for [CommunityHubScreenContent]: the gated tab row (hidden for a single live
 * segment, rendered for several), the pinned Support quick access, and the placeholder body.
 */
class CommunityHubScreenUiTest : BisqComposeUiTestBase() {
    private val actions = mutableListOf<CommunityHubUiAction>()

    private fun setContent(uiState: CommunityHubUiState) {
        actions.clear()
        setTestContent {
            CommunityHubScreenContent(uiState = uiState, onAction = actions::add)
        }
    }

    private fun tabLabel(key: String) = key.i18n()

    @Test
    fun `single live segment renders no tab row but shows support row and placeholder`() {
        setContent(
            CommunityHubUiState(
                liveSegments = listOf(CommunitySegment.DISCUSSIONS),
                selectedSegment = CommunitySegment.DISCUSSIONS,
            ),
        )

        composeTestRule.onNodeWithText(tabLabel("mobile.community.tab.messages")).assertDoesNotExist()
        composeTestRule.onNodeWithText(tabLabel("mobile.community.support.needHelp")).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.community.comingSoon".i18n(), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `multiple live segments render the tab row and tapping one selects it`() {
        setContent(
            CommunityHubUiState(
                liveSegments = CommunitySegment.entries.toList(),
                selectedSegment = CommunitySegment.DISCUSSIONS,
            ),
        )

        composeTestRule.onNodeWithText(tabLabel("mobile.community.tab.contacts")).assertIsDisplayed()
        composeTestRule.onNodeWithText(tabLabel("mobile.community.tab.messages")).performClick()

        assertEquals(listOf<CommunityHubUiAction>(CommunityHubUiAction.OnSegmentSelect(CommunitySegment.MESSAGES)), actions)
    }

    @Test
    fun `support row tap emits the open support action`() {
        setContent(
            CommunityHubUiState(
                liveSegments = listOf(CommunitySegment.DISCUSSIONS),
                selectedSegment = CommunitySegment.DISCUSSIONS,
            ),
        )

        composeTestRule.onNodeWithText(tabLabel("mobile.community.support.needHelp")).performClick()

        assertEquals(listOf<CommunityHubUiAction>(CommunityHubUiAction.OnOpenSupportChannel), actions)
    }

    @Test
    fun `no live segments renders the bare coming soon state`() {
        setContent(CommunityHubUiState())

        composeTestRule.onNodeWithText("mobile.community.comingSoon".i18n()).assertIsDisplayed()
    }

    /** The pinned Support row is Discussions-context only — directory/inbox segments drop it. */
    @Test
    fun `support row is hidden on the contacts segment`() {
        setContent(
            CommunityHubUiState(
                liveSegments = CommunitySegment.entries.toList(),
                selectedSegment = CommunitySegment.CONTACTS,
            ),
        )

        composeTestRule.onNodeWithText(tabLabel("mobile.community.support.needHelp")).assertDoesNotExist()
        composeTestRule.onNodeWithText(tabLabel("mobile.community.tab.contacts")).assertIsDisplayed()
    }
}
