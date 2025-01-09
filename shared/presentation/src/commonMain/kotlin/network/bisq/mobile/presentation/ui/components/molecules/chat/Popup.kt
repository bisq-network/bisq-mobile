package network.bisq.mobile.presentation.ui.components.molecules.chat

// import androidx.compose.foundation.layout.ColumnScopeInstance.align
// import androidx.compose.foundation.layout.FlowColumnScopeInstance.align
// import androidx.compose.foundation.layout.BoxScopeInstance.align
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.FlagIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.ReplyIcon
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.ChatPresenter
import org.koin.compose.koinInject

@Composable
fun ChatPopup(
    message: ChatMessage,
    menuPosition: Alignment.Horizontal,
    showMenu: Boolean = false,
    onSetShowMenu: (Boolean) -> Unit,
    onQuoteMessage: (ChatMessage) -> Unit = {}
) {
    val presenter: ChatPresenter = koinInject()
    val clipboardManager = LocalClipboardManager.current
    Surface(/*modifier = Modifier.align(menuPosition)*/) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onSetShowMenu(false) },
            containerColor = BisqTheme.colors.dark5
        ) {
            ChatReactionInput(
                onReact = { reaction ->
                    presenter.addReactions(reaction.fileName, message)
                    onSetShowMenu(false)
                }
            )

            DropdownMenuItem(
                text = { BisqText.smallRegular(text = "Reply") },
                leadingIcon = { ReplyIcon() },
                onClick = {
                    onQuoteMessage(message)
                    onSetShowMenu(false)
                }
            )
            DropdownMenuItem(
                text = { BisqText.smallRegular(text = "Copy") },
                leadingIcon = { CopyIcon() },
                onClick = {
                    clipboardManager.setText(buildAnnotatedString { append(message.content) })
                }
            )
            DropdownMenuItem(
                text = { BisqText.smallRegular(text = "Ignore user") },
                leadingIcon = { ClosedEyeIcon() },
                onClick = {} // TODO:
            )
            DropdownMenuItem(
                text = { BisqText.smallRegular(text = "Report user to moderator") },
                leadingIcon = { FlagIcon() },
                onClick = {} // TODO:
            )
        }
    }
}
