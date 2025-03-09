package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.i18n.i18n
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
            containerColor = BisqTheme.colors.dark4
        ) {
            ChatReactionInput(
                onReact = { reaction ->
                    presenter.addReactions(reaction.fileName, message)
                    onSetShowMenu(false)
                }
            )

            DropdownMenuItem(
                text = { BisqText.smallRegular("chat.message.reply".i18n()) },
                leadingIcon = { ReplyIcon() },
                onClick = {
                    onQuoteMessage(message)
                    onSetShowMenu(false)
                }
            )
            DropdownMenuItem(
                text = { BisqText.smallRegular("Copy") }, // TODO:i18n
                leadingIcon = { CopyIcon() },
                onClick = {
                    clipboardManager.setText(buildAnnotatedString { append(message.content) })
                }
            )
            DropdownMenuItem(
                text = { BisqText.smallRegular("chat.message.contextMenu.ignoreUser".i18n()) },
                leadingIcon = { ClosedEyeIcon() },
                onClick = {} // TODO:
            )
            DropdownMenuItem(
                text = { BisqText.smallRegular("chat.message.contextMenu.reportUser".i18n()) },
                leadingIcon = { FlagIcon() },
                onClick = {} // TODO:
            )
        }
    }
}
