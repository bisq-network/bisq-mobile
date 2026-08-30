package network.bisq.mobile.presentation.tabs.my_trades.open.components

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The row is a tap target in its own right — it opens the trade — so the peer profile link must stay
 * confined to the avatar. These two tests pin both halves of that split.
 */
class OpenTradeListItemUiTest : PresentationKoinComposeTestBase() {
    private val peerName = "Satoshi"
    private val item: TradeItemPresentationModel =
        createMockTradeItem(tradeRole = TradeRoleEnum.BUYER_AS_TAKER, peerName = peerName)

    private var selected = false
    private var peerProfileOpened = false

    private fun renderItem(
        unreadCount: Int = 0,
        isOutOfSync: Boolean = false,
    ) {
        setTestContent {
            OpenTradeListItem(
                item = item,
                userProfileIconProvider = { createEmptyImage() },
                unreadCount = unreadCount,
                onSelect = { selected = true },
                onPeerProfileClick = { peerProfileOpened = true },
                isOutOfSync = isOutOfSync,
            )
        }
    }

    @Test
    fun `when the peer avatar is tapped then the peer profile is opened`() {
        renderItem()

        composeTestRule.onNodeWithContentDescription("mobile.createProfile.iconGenerated".i18n()).performClick()
        composeTestRule.waitForIdle()

        assertTrue(peerProfileOpened)
        assertFalse(selected)
    }

    @Test
    fun `when the row is tapped outside the avatar then the trade is opened`() {
        renderItem()

        // The peer's name sits directly beside the avatar — the likeliest mis-tap.
        composeTestRule.onNodeWithText(peerName).performClick()
        composeTestRule.waitForIdle()

        assertTrue(selected)
        assertFalse(peerProfileOpened)
    }

    @Test
    fun `an out of sync trade shows the tag`() {
        renderItem(isOutOfSync = true)

        composeTestRule.onNodeWithText("mobile.bisqEasy.openTrades.outOfSync.listTag".i18n()).assertExists()
    }

    @Test
    fun `a healthy trade shows no out of sync tag`() {
        renderItem(isOutOfSync = false)

        composeTestRule.onNodeWithText("mobile.bisqEasy.openTrades.outOfSync.listTag".i18n()).assertDoesNotExist()
    }
}
