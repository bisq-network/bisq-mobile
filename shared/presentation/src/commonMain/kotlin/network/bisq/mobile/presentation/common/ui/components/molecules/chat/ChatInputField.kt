package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SendIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

private const val MAX_CHAT_INPUT_LENGTH = 10_000

@Composable
fun ChatInputField(
    onMessageSend: (String) -> Unit,
    quotedMessage: BisqEasyOpenTradeMessageModel? = null,
    placeholder: String = EMPTY_STRING,
    resetScroll: () -> Unit = {},
    onCloseReply: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf("") }
    val validationMessage =
        if (text.length > MAX_CHAT_INPUT_LENGTH) "mobile.tradeChat.chatInput.maxLength".i18n(MAX_CHAT_INPUT_LENGTH) else null
    val isTextValid = validationMessage == null

    Column {
        if (quotedMessage != null) {
            QuotedMessage(quotedMessage, onCloseReply)
        }
        BisqTextFieldV0(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.focusRequester(focusRequester),
            placeholder = placeholder,
            trailingIcon = {
                BisqIconButton(
                    onClick = {
                        if (text.isNotBlank() && isTextValid) {
                            onMessageSend(text)
                            resetScroll()
                            text = ""
                        }
                    },
                    disabled = text.isBlank() || !isTextValid,
                ) {
                    SendIcon()
                }
            },
            minLines = 1,
            maxLines = Int.MAX_VALUE,
            isError = !isTextValid,
            bottomMessage = validationMessage,
        )
    }
}

@Composable
fun QuotedMessage(
    quotedMessage: BisqEasyOpenTradeMessageModel,
    onCloseReply: () -> Unit = {},
) {
    AnimatedVisibility(visible = quotedMessage.text != null) {
        Box(
            modifier =
                Modifier
                    .padding(top = BisqUIConstants.ScreenPaddingHalf)
                    .clip(
                        shape =
                            RoundedCornerShape(
                                topStart = BisqUIConstants.ScreenPaddingHalf,
                                topEnd = BisqUIConstants.ScreenPaddingHalf,
                            ),
                    ).background(BisqTheme.colors.dark_grey10)
                    .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // todo add profile icon
                    BisqText.BaseRegular(quotedMessage.senderUserName, color = BisqTheme.colors.light_grey10)
                    CloseIconButton(onClick = onCloseReply)
                }
                BisqText.BaseLight(quotedMessage.textString, color = BisqTheme.colors.light_grey30)
            }
        }
    }
}
