package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.SendIcon
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqChatInputField(
    value: String,
    onValueChanged: (String) -> Unit = {},
    placeholder: String = "",
    onMessageSent: (String, ChatMessage?) -> Unit = { _: String, _: ChatMessage? -> },
    resetScroll: () -> Unit = {},
    quotedMessage: ChatMessage? = null,
    onCloseReply: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var textState by remember { mutableStateOf(value) }
    var textValid by remember { mutableStateOf(true) }

    Column {
        if (quotedMessage != null) {
            QuotedMessage(quotedMessage, onCloseReply)
        }
        BisqTextField(
            value = textState,
            onValueChange = { it, isValid ->
                textState = it
                textValid = isValid
                onValueChanged(textState)
            },
            indicatorColor = Color.Unspecified,
            isTextArea = true,
            modifier = Modifier.focusRequester(focusRequester),
            placeholder = placeholder,
            rightSuffix = {
                BisqIconButton(
                    onClick = {
                        if (textState.isNotEmpty() && textValid) {
                            onMessageSent(textState, quotedMessage)
                            resetScroll()
                            textState = ""
                        }
                    },
                    disabled = textState.isEmpty() || !textValid
                ) {
                    SendIcon()
                }
            },
            rightSuffixModifier = Modifier.width(BisqUIConstants.ScreenPadding2X),
            validation = {
                if (it.length > 10000) {
                    return@BisqTextField "Max length: 10,000 characters" //TODO:i18n
                }
                return@BisqTextField null
            }
        )

    }
}

@Composable
fun QuotedMessage(
    quotedMessage: ChatMessage,
    onCloseReply: () -> Unit = {},
) {
    val sideBorderColor = BisqTheme.colors.grey2
    AnimatedVisibility(visible = quotedMessage.content.isNotEmpty()) {
        Box(
            modifier = Modifier
                .padding(top = BisqUIConstants.ScreenPaddingHalf)
                .clip(
                    shape = RoundedCornerShape(
                        topStart = BisqUIConstants.ScreenPaddingHalf,
                        topEnd = BisqUIConstants.ScreenPaddingHalf,
                    )
                )
                .background(BisqTheme.colors.grey3)
                .drawBehind {
                    drawLine(
                        color = sideBorderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 10.dp.toPx()
                    )
                }
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
                    BisqText.baseMedium(quotedMessage.author)
                    CloseIconButton(onClick = onCloseReply)
                }
                BisqText.baseMedium(quotedMessage.content)
            }
        }
    }
}
