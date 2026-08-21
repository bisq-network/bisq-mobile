package network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LeaveChatIcon

/**
 * Leaves a private chat, shown as a TopBar action.
 *
 * Does not act immediately: tapping only opens a confirmation dialog. Leaving deletes the local
 * conversation irreversibly, so it is deliberately a two-step action.
 *
 * The description names the action and sits on the tap target; the icon is hidden from accessibility
 * so its own "Leave chat icon" — a noun, and a duplicate — is not what a screen reader announces. It
 * reuses the key the confirmation dialog already shows, because an icon-only button is the one place
 * where the accessible name *is* the label, and a screen reader on a German device must not be the
 * only surface still reading English.
 */
@Composable
fun LeaveChatIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .testTag("leave_private_chat_button")
                .semantics { contentDescription = "mobile.privateChats.chat.leaveChat".i18n() },
    ) {
        LeaveChatIcon(modifier = Modifier.size(24.dp).clearAndSetSemantics { hideFromAccessibility() })
    }
}
