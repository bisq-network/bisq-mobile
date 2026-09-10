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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.chat.ChatMentionParser
import network.bisq.mobile.data.replicated.chat.ChatMessage
import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.BackHandler
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SaveIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SendIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

private const val MAX_CHAT_INPUT_LENGTH = 10_000

@Composable
fun ChatInputField(
    onMessageSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    quotedMessage: ChatMessage<*>? = null,
    placeholder: String = EMPTY_STRING,
    resetScroll: () -> Unit = {},
    onCloseReply: () -> Unit = {},
    sendEnabled: Boolean = true,
    editingMessageId: String? = null,
    editingInitialText: String = "",
    onCancelEdit: () -> Unit = {},
    mentionCandidates: List<UserProfileVO> = emptyList(),
) {
    val focusRequester = remember { FocusRequester() }
    val isEditing = editingMessageId != null
    // Re-keyed on the edited message, so entering, switching and leaving an edit all reload the
    // composer. Known trade-off, matching desktop: an unsent draft is lost on entering an edit.
    // The value carries its selection because an edit opens with the cursor after the text, and the
    // plain-string field has no say over where the cursor goes: it always starts at zero.
    var textFieldValue by remember(editingMessageId) {
        mutableStateOf(TextFieldValue(editingInitialText, TextRange(editingInitialText.length)))
    }
    val text = textFieldValue.text
    // Entering an edit is a command to type, so the composer takes the focus — which opens the
    // keyboard — rather than waiting for a tap on a field that is already full of text.
    LaunchedEffect(editingMessageId) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
    val validationMessage =
        if (text.length > MAX_CHAT_INPUT_LENGTH) "mobile.tradeChat.chatInput.maxLength".i18n(MAX_CHAT_INPUT_LENGTH) else null
    val isTextValid = validationMessage == null
    val mentionMatch =
        remember(textFieldValue, isEditing) {
            if (isEditing) {
                null
            } else {
                ChatMentionParser.findMentionAtCaret(textFieldValue.text, textFieldValue.selection.end)
            }
        }
    val mentionSuggestions =
        remember(mentionMatch, mentionCandidates) {
            val match = mentionMatch ?: return@remember emptyList()
            ChatMentionParser.filterAndSort(mentionCandidates, match.query)
        }
    var dismissedIndicatorIndex by remember { mutableStateOf<Int?>(null) }
    val mentionIndicatorIndex = mentionMatch?.indicatorIndex
    LaunchedEffect(mentionIndicatorIndex) {
        if (mentionIndicatorIndex == null) {
            dismissedIndicatorIndex = null
        } else if (dismissedIndicatorIndex != null && dismissedIndicatorIndex != mentionIndicatorIndex) {
            dismissedIndicatorIndex = null
        }
    }
    val activeMention = mentionMatch
    val showMentionPicker =
        activeMention != null &&
            mentionCandidates.isNotEmpty() &&
            dismissedIndicatorIndex != activeMention.indicatorIndex

    Column(modifier = modifier) {
        if (activeMention != null && showMentionPicker) {
            BackHandler {
                dismissedIndicatorIndex = activeMention.indicatorIndex
            }
            ChatMentionPicker(
                profiles = mentionSuggestions,
                onSelect = { profile ->
                    // Close this token before applying the insert. Insertion before punctuation
                    // leaves the caret on the name, and findMentionAtCaret would otherwise
                    // match again and reopen the picker.
                    dismissedIndicatorIndex = activeMention.indicatorIndex
                    val insertion = ChatMentionParser.insertMention(textFieldValue.text, activeMention, profile.userName)
                    textFieldValue = TextFieldValue(insertion.text, TextRange(insertion.caretPosition))
                },
            )
        }
        // Mutually exclusive: bisq2 keeps the original's citation on an edit, so the quote banner has
        // nothing to offer while editing.
        if (isEditing) {
            EditingMessageBanner(onCancelEdit)
        } else if (quotedMessage != null) {
            QuotedMessage(quotedMessage, onCloseReply)
        }
        BisqTextFieldV0(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier = Modifier.focusRequester(focusRequester),
            placeholder = placeholder,
            trailingIcon = {
                BisqIconButton(
                    onClick = {
                        if (text.isNotBlank() && isTextValid) {
                            onMessageSend(text)
                            resetScroll()
                            // Cleared for a send only. A save can be refused — a rate limit, a removal
                            // the local store rejects — and the presenter then keeps the edit open, so
                            // clearing here would strand the banner over an empty field with Save
                            // disabled and the user's text gone. On success clearEditing() re-keys the
                            // remember below, which empties the field anyway.
                            if (!isEditing) {
                                textFieldValue = TextFieldValue()
                            }
                        }
                    },
                    disabled = text.isBlank() || !isTextValid || !sendEnabled,
                ) {
                    if (isEditing) SaveIcon() else SendIcon()
                }
            },
            minLines = 1,
            maxLines = Int.MAX_VALUE,
            isError = !isTextValid,
            bottomMessage = validationMessage,
        )
    }
}

/**
 * Editing reuses the composer rather than a dialog or an in-bubble field: the composer already owns
 * the character cap and its inline validation, the multiline growth and the blank-send disabling.
 */
@Composable
private fun EditingMessageBanner(onCancelEdit: () -> Unit) {
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.BaseRegular("action.edit".i18n(), color = BisqTheme.colors.light_grey10)
            CloseIconButton(onClick = onCancelEdit)
        }
    }
}

@Composable
fun QuotedMessage(
    quotedMessage: ChatMessage<*>,
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

@Preview
@Composable
private fun ChatInputField_MentionCandidatesPreview() {
    BisqTheme.Preview {
        ChatInputField(
            onMessageSend = {},
            placeholder = "chat.message.input.prompt".i18n(),
            // The picker is caret-anchored: an empty field has no @ token, so nothing to draw.
            editingInitialText = "@",
            mentionCandidates =
                listOf(
                    createMockUserProfile("Alice"),
                    createMockUserProfile("Bob"),
                ),
        )
    }
}

@Preview
@Composable
private fun ChatInputField_EmptyPreview() {
    BisqTheme.Preview {
        ChatInputField(
            onMessageSend = {},
            placeholder = "chat.message.input.prompt".i18n(),
        )
    }
}

@Preview
@Composable
private fun ChatInputField_WithQuotedMessagePreview() {
    BisqTheme.Preview {
        ChatInputField(
            onMessageSend = {},
            quotedMessage = previewQuotedMessage("Sure! Let's proceed with the payment.", "Alice"),
            placeholder = "chat.message.input.prompt".i18n(),
        )
    }
}

@Preview
@Composable
private fun QuotedMessage_LongTextPreview() {
    BisqTheme.Preview {
        QuotedMessage(
            quotedMessage =
                previewQuotedMessage(
                    text =
                        "I sent the payment a few minutes ago, the reference should show up on your " +
                            "statement as the trade id. Let me know once you see it and I will confirm here.",
                    senderName = "SatoshiNakamotoLongNickname",
                ),
        )
    }
}

@ExcludeFromCoverage
private fun previewQuotedMessage(
    text: String,
    senderName: String,
) = createMockTwoPartyPrivateChatMessage(
    id = "msg1",
    text = text,
    senderUserProfile = createMockUserProfile(senderName),
    myUserProfile = createMockUserProfile("Bob"),
)
