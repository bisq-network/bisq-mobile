package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.AnimatedBadge

/**
 * The one unread-count → badge-pill contract, shared by every unread badge:
 * hidden at zero or below, exact count up to 99, capped at "99+" above.
 */
fun formatUnreadBadgeCount(count: Int): String? =
    when {
        count <= 0 -> null
        count > 99 -> "99+"
        else -> count.toString()
    }

/**
 * Unread-count badge pill. Renders nothing when [count] is zero or below, so call
 * sites don't need their own visibility check.
 */
@Composable
fun UnreadCountBadge(
    count: Int,
    showAnimation: Boolean = false,
) {
    val text = formatUnreadBadgeCount(count) ?: return
    AnimatedBadge(text = text, showAnimation = showAnimation)
}
