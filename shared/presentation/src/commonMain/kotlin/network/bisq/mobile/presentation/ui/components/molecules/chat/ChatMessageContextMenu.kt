package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.FlagIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.ReplyIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun ChatMessageContextMenu(
    message: BisqEasyOpenTradeMessageModel,
    menuPosition: Alignment.Horizontal,
    showMenu: Boolean = false,
    onSetShowMenu: (Boolean) -> Unit,
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit,
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onIgnoreUser: () -> Unit = {},
    onReportUser: () -> Unit = {}
) {
    val isPeersMessage = !message.isMyMessage
    Surface(/*modifier = Modifier.align(menuPosition)*/) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onSetShowMenu(false) },
            containerColor = BisqTheme.colors.dark4
        ) {
            ChatReactionInput(
                onAddReaction = { reaction ->
                    onAddReaction(reaction)
                    onSetShowMenu(false)
                },
                onRemoveReaction = { reaction ->
                    onRemoveReaction(reaction)
                    onSetShowMenu(false)
                }
            )

            HorizontalDivider(
                color = BisqTheme.colors.dark5,
                thickness = 2.dp
            )

            if (isPeersMessage) {
                DropdownMenuItem(
                    text = { BisqText.smallRegular("chat.message.reply".i18n()) },
                    leadingIcon = { ReplyIcon() },
                    onClick = {
                        onReply()
                        onSetShowMenu(false)
                    }
                )
            }
            DropdownMenuItem(
                text = { BisqText.smallRegular("action.copyToClipboard".i18n()) },
                leadingIcon = { CopyIcon() },
                onClick = {
                    onCopy()
                }
            )
            if (isPeersMessage) {
                DropdownMenuItem(
                    text = { BisqText.smallRegular("chat.message.contextMenu.ignoreUser".i18n()) },
                    leadingIcon = { ClosedEyeIcon() },
                    onClick = {
                        onIgnoreUser()
                        onSetShowMenu(false)
                    }
                )
                DropdownMenuItem(
                    text = { BisqText.smallRegular("chat.message.contextMenu.reportUser".i18n()) },
                    leadingIcon = { FlagIcon() },
                    onClick = {
                        onReportUser()
                        onSetShowMenu(false)
                    }
                )
            }
        }
    }
}
