package network.bisq.mobile.presentation.community

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ChatIconOutlined
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.UnreadCountBadge
import network.bisq.mobile.presentation.common.ui.components.molecules.formatUnreadBadgeCount
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

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
    // Activity is signalled by the badge, never by the glyph colour — and the glyph must not
    // borrow a state colour either: primary green means selected/connected/positive everywhere
    // else in this app (bottom-nav selection, connectivity dot), so a permanently green icon
    // would read as a false "active" signal. mid_grey20 keeps it neutral chrome with more
    // visual weight than stark white. Sized to the avatar so the two centre together and sit
    // close — a default 48dp IconButton adds ~12dp of invisible padding around the 24dp glyph.
    val tint = ColorFilter.tint(BisqTheme.colors.mid_grey20)
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(BisqUIConstants.topBarAvatarSize)
                // Optical alignment using offset as is a post-layout translation — unlike
                // padding it leaves touch bounds and the badgeanchor untouched.
                .offset(y = 1.dp)
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
                ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X), colorFilter = tint)
            }
        } else {
            ChatIconOutlined(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X), colorFilter = tint)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityTopBarIcon_NoUnread_Preview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Offerbook",
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 0, showAnimation = false, onClick = {}) },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityTopBarIcon_SmallUnread_Preview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Offerbook",
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 5, showAnimation = false, onClick = {}) },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityTopBarIcon_CappedUnread_Preview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Offerbook",
            showUserAvatar = true,
            extraActions = { CommunityTopBarIcon(unreadCount = 120, showAnimation = false, onClick = {}) },
        )
    }
}
