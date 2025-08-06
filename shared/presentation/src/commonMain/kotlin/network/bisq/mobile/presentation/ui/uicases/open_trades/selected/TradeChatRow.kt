package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants


@Composable
fun TradeChatRow(
    presenter: OpenTradePresenter,
    lastChatMsg: BisqEasyOpenTradeMessageModel?,
    newMsgCount: Int = 0,
    enabled: Boolean = true,
) {
    val text = remember(lastChatMsg, presenter.selectedTrade.value) {
        if (lastChatMsg == null) {
            ""
        } else {
            when (lastChatMsg.chatMessageType) {
                ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE -> lastChatMsg.decodedText
                ChatMessageTypeEnum.LEAVE -> "bisqEasy.openTrades.chat.peerLeft.headline".i18n(
                    lastChatMsg.senderUserName
                )

                else ->
                    if (lastChatMsg.text != null) {
                        val sender =
                            if (lastChatMsg.senderUserProfile.id == presenter.selectedTrade.value?.myUserProfile?.id) {
                                "mobile.tradeChat.sender.me".i18n()
                            } else {
                                lastChatMsg.senderUserProfile.nickName
                            }
                        sender + ": " + lastChatMsg.text
                    } else {
                        ""
                    }
            }
        }
    }

    Box(
        Modifier.background(
            color = BisqTheme.colors.dark_grey40,
            shape = RoundedCornerShape(BisqUIConstants.ScreenPadding5X)
        ).clickable(onClick = {
            presenter.onOpenChat()
        })
    )
    {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = BisqUIConstants.ScreenPadding),
        ) {
            BisqText.styledText(
                text = text,
                color = BisqTheme.colors.mid_grey20,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            BadgedBox(
                modifier = Modifier.graphicsLayer(clip = false),
                badge = {
                    if (newMsgCount > 0) {
                        AnimatedBadge(showAnimation = true, xOffset = (-4).dp) {
                            BisqText.xsmallLight(
                                newMsgCount.toString(),
                                textAlign = TextAlign.Center, color = BisqTheme.colors.dark_grey20
                            )
                        }
                    }
                }) {
                IconButton(
                    enabled = enabled,
                    onClick = { presenter.onOpenChat() },
                    colors = IconButtonColors(
                        containerColor = BisqTheme.colors.primary,
                        disabledContainerColor = BisqTheme.colors.primaryDisabled,
                        contentColor = BisqTheme.colors.white,
                        disabledContentColor = BisqTheme.colors.mid_grey20,
                    ),
                ) {
                    ChatIcon(modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}