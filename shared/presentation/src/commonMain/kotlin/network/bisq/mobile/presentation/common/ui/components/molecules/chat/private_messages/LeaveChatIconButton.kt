package network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LeaveChatIcon

/**
 * Leaves a private chat, shown as a TopBar action.
 *
 * Does not act immediately: tapping only opens a confirmation dialog. Leaving deletes the local
 * conversation irreversibly, so it is deliberately a two-step action.
 */
@Composable
fun LeaveChatIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("leave_private_chat_button"),
    ) {
        LeaveChatIcon()
    }
}
