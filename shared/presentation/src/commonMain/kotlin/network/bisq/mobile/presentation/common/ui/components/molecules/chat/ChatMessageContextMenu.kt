package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ClosedEyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.EyeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.FlagIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ReplyIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ChatMessageContextMenu(
    message: BisqEasyOpenTradeMessageModel,
    showMenu: Boolean = false,
    onSetShowMenu: (Boolean) -> Unit,
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit,
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onIgnoreUser: () -> Unit = {},
    onUndoIgnoreUser: () -> Unit = {},
    onReportUser: () -> Unit = {},
    isIgnored: Boolean,
) {
    val isPeersMessage = !message.isMyMessage
    Surface {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onSetShowMenu(false) },
            containerColor = BisqTheme.colors.dark_grey40
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
                color = BisqTheme.colors.dark_grey50,
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
                if (isIgnored) {
                    DropdownMenuItem(
                        text = { BisqText.smallRegular("user.profileCard.userActions.undoIgnore".i18n()) },
                        leadingIcon = { EyeIcon() },
                        onClick = {
                            onUndoIgnoreUser()
                            onSetShowMenu(false)
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { BisqText.smallRegular("chat.message.contextMenu.ignoreUser".i18n()) },
                        leadingIcon = { ClosedEyeIcon() },
                        onClick = {
                            onIgnoreUser()
                            onSetShowMenu(false)
                        }
                    )
                }

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

@Preview
@Composable
private fun ChatMessageContextMenuPreview() {
    BisqTheme.Preview {
        ChatMessageContextMenu(
            message = mockMessage,
            showMenu = true,
            onSetShowMenu = {},
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false
        )
    }
}

@Preview
@Composable
private fun ChatMessageContextMenuIgnoredPreview() {
    BisqTheme.Preview {
        ChatMessageContextMenu(
            message = mockMessage,
            showMenu = true,
            onSetShowMenu = {},
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = true
        )
    }
}

private val mockMessage by lazy {
    val myUserProfile = createMockUserProfile("Bob")
    val peerUserProfile = createMockUserProfile("Alice")

    val dto = BisqEasyOpenTradeMessageDto(
        tradeId = "trade123",
        messageId = "msg456",
        channelId = "channel123",
        senderUserProfile = peerUserProfile,
        receiverUserProfileId = myUserProfile.networkId.pubKey.id,
        receiverNetworkId = myUserProfile.networkId,
        text = "Sure! Let's proceed with the payment.",
        citation = null,
        date = 1234567890000L,
        mediator = null,
        chatMessageType = ChatMessageTypeEnum.TEXT,
        bisqEasyOffer = null,
        chatMessageReactions = emptySet(),
        citationAuthorUserProfile = null
    )

    BisqEasyOpenTradeMessageModel(
        dto,
        myUserProfile,
        emptyList()
    )
}
