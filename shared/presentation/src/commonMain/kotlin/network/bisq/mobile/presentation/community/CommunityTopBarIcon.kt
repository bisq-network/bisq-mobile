package network.bisq.mobile.presentation.community

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ChatIconOutlined
import network.bisq.mobile.presentation.common.ui.components.molecules.UnreadCountBadge
import network.bisq.mobile.presentation.common.ui.components.molecules.formatUnreadBadgeCount
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * The global Community entry point, rendered in TopBar's `extraActions` slot on every main
 * tab.
 *
 * @param unreadCount the aggregate community unread count ([UnreadCountBadge] contract:
 *   hidden at zero, capped at "99+").
 * @param showAnimation whether the badge pulses — follows the same AnimationSettings-gated
 *   flag as the bottom-nav badge, not a separate toggle.
 * @param onClick navigates to the Community hub.
 */
@Composable
fun CommunityTopBarIcon(
    unreadCount: Int,
    showAnimation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = "mobile.community.entry.contentDescription".i18n()
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .testTag("community_topbar_icon")
                .semantics { contentDescription = label },
    ) {
        val badgeText = formatUnreadBadgeCount(unreadCount)
        if (badgeText != null) {
            BadgedBox(
                badge = {
                    // Near-zero offsets: AnimatedBadge's defaults are tuned for the bottom
                    // nav's roomy Column; TopAppBar is a fixed-height clipping Surface that
                    // would cut the pill off with them.
                    AnimatedBadge(
                        text = badgeText,
                        showAnimation = showAnimation,
                        xOffset = 2.dp,
                        yOffset = (-2).dp,
                    )
                },
                modifier = Modifier.padding(top = 2.dp, end = 2.dp),
            ) {
                ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
            }
        } else {
            ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
        }
    }
}
