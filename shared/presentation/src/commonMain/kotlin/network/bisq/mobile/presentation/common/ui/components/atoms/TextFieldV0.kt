package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CheckCircleIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SearchIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BisqTextFieldV0(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    color: Color = BisqTheme.colors.light_grey20,
    textStyle: TextStyle =
        TextStyle(
            color = color,
            fontSize = 18.sp,
            textDecoration = TextDecoration.None,
        ),
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    bottomMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
) {
    val secondaryColor = BisqTheme.colors.secondary
    val secondaryHoverColor = BisqTheme.colors.secondaryHover
    val secondaryDisabledColor = BisqTheme.colors.secondaryDisabled
    val dangerColor = BisqTheme.colors.danger
    val greyColor = BisqTheme.colors.mid_grey20
    val primaryColor = BisqTheme.colors.primary

    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor =
        when {
            !enabled -> secondaryDisabledColor
            isFocused -> secondaryHoverColor
            else -> secondaryColor
        }

    val indicatorColor =
        when {
            isError -> dangerColor
            isFocused -> primaryColor
            else -> greyColor
        }

    val bottomMessageColor =
        when {
            isError -> dangerColor
            else -> greyColor
        }

    val labelColor =
        when {
            isError -> dangerColor
            else -> BisqTheme.colors.white
        }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(BisqTheme.colors.primary),
    ) { innerTextField ->
        Column {
            if (!label.isNullOrBlank()) {
                BisqText.BaseLight(
                    color = labelColor,
                    text = label,
                    modifier = Modifier.padding(4.dp),
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = BisqUIConstants.BorderRadius,
                                topEnd = BisqUIConstants.BorderRadius,
                            ),
                        ).background(backgroundColor)
                        .drawBehind {
                            val strokeWidth = 4.dp.toPx()
                            val y = size.height
                            drawLine(
                                color = indicatorColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth,
                            )
                        }.padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon?.let { icon ->
                        icon()
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        prefix?.let { prefixContent ->
                            prefixContent()
                        }

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f),
                            propagateMinConstraints = true,
                        ) {
                            if (value.isEmpty() && placeholder != null) {
                                BisqText.LargeLightGrey(placeholder)
                            }
                            innerTextField()
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        suffix?.let { suffixContent ->
                            suffixContent()
                        }
                    }

                    trailingIcon?.let { icon ->
                        icon()
                    }
                }
            }

            if (!bottomMessage.isNullOrBlank()) {
                BisqText.SmallLight(
                    text = bottomMessage,
                    color = bottomMessageColor,
                    modifier =
                        Modifier
                            .padding(4.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_DefaultPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Sample text") }
            BisqTextFieldV0(
                label = "Label",
                value = textState,
                onValueChange = { textState = it },
                bottomMessage = "This is a bottom message",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_EmptyPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("") }
            BisqTextFieldV0(
                label = "Empty Field",
                value = textState,
                onValueChange = { textState = it },
                placeholder = "Enter some text here...",
                bottomMessage = "Enter some text",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_ErrorPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Invalid input") }
            BisqTextFieldV0(
                label = "Username",
                value = textState,
                onValueChange = { textState = it },
                isError = true,
                bottomMessage = "Username is already taken",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_DisabledPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Disabled field") }
            BisqTextFieldV0(
                label = "Read-only Field",
                value = textState,
                onValueChange = { textState = it },
                enabled = false,
                bottomMessage = "This field cannot be edited",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_NoLabelPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Text without label") }
            BisqTextFieldV0(
                value = textState,
                onValueChange = { textState = it },
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_MultiLinePreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember {
                mutableStateOf("This is a multi-line text field.\nIt can contain multiple lines of text.\nYou can add as many lines as you want.")
            }
            BisqTextFieldV0(
                label = "Description",
                value = textState,
                onValueChange = { textState = it },
                minLines = 3,
                maxLines = 5,
                bottomMessage = "Maximum 500 characters",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_SingleLinePreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Single line input") }
            BisqTextFieldV0(
                label = "Email",
                value = textState,
                onValueChange = { textState = it },
                singleLine = true,
                bottomMessage = "Enter your email address",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithPlaceholderPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("") }
            BisqTextFieldV0(
                label = "Optional Field",
                value = textState,
                onValueChange = { textState = it },
                placeholder = "Type something...",
                bottomMessage = "This field is optional",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_LabelOnlyPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Some text") }
            BisqTextFieldV0(
                label = "Simple Label",
                value = textState,
                onValueChange = { textState = it },
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithPrefixPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("example.com") }
            BisqTextFieldV0(
                label = "Website",
                value = textState,
                onValueChange = { textState = it },
                prefix = {
                    BisqText.BaseLight(
                        text = "https://",
                        color = BisqTheme.colors.mid_grey20,
                    )
                },
                bottomMessage = "Enter your website domain",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithSuffixPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("100") }
            BisqTextFieldV0(
                label = "Amount",
                value = textState,
                onValueChange = { textState = it },
                suffix = {
                    BisqText.BaseLight(
                        text = "BTC",
                        color = BisqTheme.colors.mid_grey20,
                    )
                },
                bottomMessage = "Enter amount in BTC",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithPrefixAndSuffixPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("1,000") }
            BisqTextFieldV0(
                label = "Price",
                value = textState,
                onValueChange = { textState = it },
                prefix = {
                    BisqText.BaseLight(
                        text = "$",
                        color = BisqTheme.colors.mid_grey20,
                    )
                },
                suffix = {
                    BisqText.BaseLight(
                        text = "USD",
                        color = BisqTheme.colors.mid_grey20,
                    )
                },
                bottomMessage = "Enter price in USD",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithLeadingIconPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("search query") }
            BisqTextFieldV0(
                label = "Search",
                value = textState,
                onValueChange = { textState = it },
                leadingIcon = {
                    SearchIcon(modifier = Modifier.size(20.dp))
                },
                bottomMessage = "Search for items",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithTrailingIconPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("Clear me") }
            BisqTextFieldV0(
                label = "Input",
                value = textState,
                onValueChange = { textState = it },
                trailingIcon = {
                    CloseIcon(modifier = Modifier.size(20.dp), color = BisqTheme.colors.mid_grey20)
                },
                bottomMessage = "Click X to clear",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldV0_WithAllDecorationsPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("This is a very long text value that should test how the text field handles lengthy input content with all decorations enabled") }
            BisqTextFieldV0(
                label = "This is a very long label text to test how the component handles lengthy label content",
                value = textState,
                onValueChange = { textState = it },
                leadingIcon = {
                    CopyIcon(modifier = Modifier.size(20.dp))
                },
                prefix = {
                    BisqText.BaseLight(
                        text = "$",
                        color = BisqTheme.colors.mid_grey20,
                    )
                },
                suffix = {
                    BisqText.BaseLight(
                        text = "USD",
                        color = BisqTheme.colors.mid_grey20,
                    )
                },
                trailingIcon = {
                    CheckCircleIcon(modifier = Modifier.size(20.dp))
                },
                bottomMessage = "This is a very long bottom message that should test how the text field component handles lengthy helper text or error messages that span multiple lines",
            )
        }
    }
}
