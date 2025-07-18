package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.SendIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ChatInputField(
    quotedMessage: BisqEasyOpenTradeMessageModel? = null,
    placeholder: String = "",
    onMessageSent: (String) -> Unit,
    resetScroll: () -> Unit = {},
    onCloseReply: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf("") }
    var isTextValid by remember { mutableStateOf(true) }

    Column {
        if (quotedMessage != null) {
            QuotedMessage(quotedMessage, onCloseReply)
        }
        BisqTextField(
            value = text,
            onValueChange = { it, isValid ->
                text = it
                isTextValid = isValid
            },
            indicatorColor = Color.Unspecified,
            isTextArea = true,
            modifier = Modifier.focusRequester(focusRequester),
            placeholder = placeholder,
            rightSuffix = {
                BisqIconButton(
                    onClick = {
                        if (text.isNotEmpty() && isTextValid) {
                            onMessageSent(text)
                            resetScroll()
                            text = ""
                        }
                    },
                    disabled = text.isEmpty() || !isTextValid
                ) {
                    SendIcon()
                }
            },
            rightSuffixModifier = Modifier.width(BisqUIConstants.ScreenPadding2X),
            validation = {
                val maxChars = 10_000
                if (it.length > maxChars) {
                    return@BisqTextField "mobile.tradeChat.chatInput.maxLength".i18n(maxChars)
                }
                return@BisqTextField null
            }
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
            modifier = Modifier
                .padding(top = BisqUIConstants.ScreenPaddingHalf)
                .clip(
                    shape = RoundedCornerShape(
                        topStart = BisqUIConstants.ScreenPaddingHalf,
                        topEnd = BisqUIConstants.ScreenPaddingHalf,
                    )
                )
                .background(BisqTheme.colors.dark_grey10)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //todo add profile icon
                    BisqText.baseRegular(quotedMessage.senderUserName, color = BisqTheme.colors.light_grey10)
                    CloseIconButton(onClick = onCloseReply)
                }
                BisqText.baseLight(quotedMessage.textString, color = BisqTheme.colors.light_grey30)
            }
        }
    }
}
