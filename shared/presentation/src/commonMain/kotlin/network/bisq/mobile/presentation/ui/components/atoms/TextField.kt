package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.domain.getDecimalSeparator
import network.bisq.mobile.presentation.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.ui.components.atoms.button.PasswordIconButton
import network.bisq.mobile.presentation.ui.components.atoms.button.PasteIconButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.context.LocalAnimationsEnabled
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants.Zero
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants.textFieldBorderRadius
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class BisqTextFieldType {
    Default,
    Transparent,
}


/**
 * TODO:
 * 1. Should have a BisqNumberField with customizations like numberWithTwoDecimals
 * and whose value is Double and onValueChange emits Double
 * 2. Add onFocusOut event, to reset data incase of invalid inputs
 */
@Composable
fun BisqTextField(
    label: String = "",
    value: String = "",
    onValueChange: ((String, Boolean) -> Unit)? = null,
    placeholder: String = "",
    labelRightSuffix: (@Composable () -> Unit)? = null,
    leftSuffix: (@Composable () -> Unit)? = null,
    rightSuffix: (@Composable () -> Unit)? = null,
    rightSuffixModifier: Modifier = Modifier.width(50.dp),
    rightSuffixContentAlignment: Alignment = Alignment.CenterEnd,
    isSearch: Boolean = false,
    helperText: String = "",
    indicatorColor: Color = BisqTheme.colors.primary,
    isTextArea: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    paddingValues: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    minLines: Int = 1,
    maxLines: Int = if (isTextArea) 4 else 1,
    maxLength: Int = 0,
    disabled: Boolean = false,
    color: Color = BisqTheme.colors.light_grey20,
    backgroundColor: Color = BisqTheme.colors.secondary,
    showCopy: Boolean = false,
    showPaste: Boolean = false,
    valuePrefix: String? = null,
    valueSuffix: String? = null,
    validation: ((String) -> String?)? = null,
    numberWithTwoDecimals: Boolean = false,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(
        color = color,
        fontSize = 18.sp,
        textDecoration = TextDecoration.None
    ),
    textFieldAlignment: Alignment = Alignment.TopStart,
    enableAnimation: Boolean = LocalAnimationsEnabled.current,
    onFocus: () -> Unit = {},
    type: BisqTextFieldType = BisqTextFieldType.Default,
    isPasswordField: Boolean = false,
) {
    var hasInteracted by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var visualTransformation by remember { mutableStateOf(VisualTransformation.None) }
    var obscurePassword by remember { mutableStateOf(true) }

    if (isPasswordField) {
        if (obscurePassword) {
            visualTransformation = PasswordVisualTransformation()
        } else {
            visualTransformation = VisualTransformation.None
        }
    }
    val animatedLineProgress by animateFloatAsState(
        targetValue = if (isFocused && enableAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "BottomBorderAnimation"
    )

    val imeAction = when {
        isSearch -> ImeAction.Search
        isTextArea -> ImeAction.Next
        else -> ImeAction.Done
    }

    val finalTextValue = buildString {
        valuePrefix?.let {
            if (!value.startsWith(valuePrefix))
                append(it)
        }
        append(value)
        valueSuffix?.let {
            if (!value.endsWith(valueSuffix))
                append(it)
        }
    }

    val dangerColor = BisqTheme.colors.danger
    val grey2Color = BisqTheme.colors.mid_grey20
    val finalIndicatorColor = when {
        !validationError.isNullOrEmpty() && hasInteracted && enableAnimation -> dangerColor
        !enableAnimation && isFocused -> indicatorColor
        else -> grey2Color
    }

    val secondaryHoverColor = BisqTheme.colors.secondaryHover
    val secondaryDisabledColor = BisqTheme.colors.secondaryDisabled
    val finalBackgroundColor by remember(disabled, isFocused, backgroundColor, type) {
        derivedStateOf {
            when {
                disabled -> secondaryDisabledColor
                isFocused -> if (type == BisqTextFieldType.Default)
                    secondaryHoverColor
                else
                    backgroundColor

                else -> backgroundColor
            }
        }
    }

    val whiteColor = BisqTheme.colors.white
    val finalLabelColor by remember(disabled, validationError, hasInteracted) {
        derivedStateOf {
            when {
                disabled -> BisqTheme.colors.mid_grey30
                isFocused -> BisqTheme.colors.primary
                validationError?.isNotEmpty() == true && hasInteracted -> BisqTheme.colors.danger
                else -> whiteColor
            }
        }
    }

    val finalTextStyle by remember(disabled, textStyle) {
        derivedStateOf {
            if (disabled) textStyle.copy(color = BisqTheme.colors.mid_grey20) else textStyle
        }
    }

    // Trigger validation for read only fields, on first render
    LaunchedEffect(disabled) {
        if (disabled && value.isNotEmpty()) {
            hasInteracted = true
            validationError = validation?.invoke(value)
        }
    }

    // Re-validate, whenever validation function itself changes
    // Applicable in cases, where the validation() changes based on
    // change in other parameters like BitcoinLnAddressField::type
    LaunchedEffect(validation) {
        if (value.isNotEmpty()) {
            hasInteracted = true
        }
        val result = validation?.invoke(value)
        validationError = result
    }

    val finalBorderRadius by remember(isFocused) {
        derivedStateOf { if (isFocused) Zero else textFieldBorderRadius }
    }

    val decimalSeparator = remember { getDecimalSeparator().toString() }
    val decimalLoosePattern = remember(decimalSeparator) {
        Regex("^[-]?\\d*(${Regex.escape(decimalSeparator)}\\d{0,})?$")
    }

    BasicTextField(
        value = finalTextValue,
        visualTransformation = visualTransformation,
        onValueChange = { newTextValue ->
            val processedValue = processText(
                newTextValue,
                finalTextValue,
                valuePrefix,
                valueSuffix,
                maxLength,
                numberWithTwoDecimals,
                decimalSeparator,
                decimalLoosePattern
            )
            if (processedValue == value) return@BasicTextField
            validationError = validation?.invoke(processedValue)
            onValueChange?.invoke(processedValue, validationError.isNullOrEmpty())
            hasInteracted = true
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
                if (!focusState.isFocused) {
                    if (value.isNotEmpty()) hasInteracted = true
                    validationError = validation?.invoke(value)
                }
            },
        enabled = !disabled,
        textStyle = finalTextStyle,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = !isTextArea,
        maxLines = maxLines,
        minLines = minLines,
        cursorBrush = SolidColor(BisqTheme.colors.primary),
        decorationBox = { innerTextField ->
            Column(modifier = Modifier.padding(bottom = 2.dp)) {
                // Label
                if (label.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BisqText.baseLight(
                            text = label,
                            color = finalLabelColor,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                        labelRightSuffix?.invoke()
                    }
                    BisqGap.VQuarter()
                }

                // The main input area with background and suffixes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = finalBorderRadius,
                                topEnd = finalBorderRadius
                            )
                        )
                        .background(finalBackgroundColor)
                        .drawBehind {
                            if (!isSearch && type == BisqTextFieldType.Default) {
                                val strokeWidth = 4.dp.toPx()
                                val y = size.height
                                drawLine(
                                    color = finalIndicatorColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeWidth
                                )
                                if (animatedLineProgress > 0f) {
                                    drawLine(
                                        color = indicatorColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width * animatedLineProgress, y),
                                        strokeWidth = strokeWidth
                                    )
                                }
                            }
                        }
                        .padding(paddingValues),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftSuffix?.invoke()
                    if (leftSuffix != null) Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = textFieldAlignment,
                    ) {
                        if (value.isEmpty() && valuePrefix.isNullOrEmpty() && valueSuffix.isNullOrEmpty()) {
                            BisqText.largeLightGrey(placeholder)
                        }
                        innerTextField()
                    }

                    if (showCopy) {
                        CopyIconButton(value)
                    }
                    if (showPaste && !disabled) {
                        PasteIconButton(onPaste = { pasted ->
                            val processed = processText(
                                newValue = pasted,
                                oldValue = value,
                                valuePrefix = valuePrefix,
                                valueSuffix = valueSuffix,
                                maxLength = maxLength,
                                numberWithTwoDecimals = numberWithTwoDecimals,
                                decimalSeparator = decimalSeparator,
                                decimalLoosePattern = decimalLoosePattern
                            )
                            validationError = validation?.invoke(processed)
                            onValueChange?.invoke(processed, validationError.isNullOrEmpty())
                            hasInteracted = true
                        })
                    }
                    if (rightSuffix != null) {
                        Box(
                            modifier = rightSuffixModifier,
                            contentAlignment = rightSuffixContentAlignment,
                        ) {
                            rightSuffix()
                        }
                    }
                    if (isPasswordField) {
                        PasswordIconButton(onObscurePassword = { obscurePassword = it })
                    }
                }

                // Error text has priority over help field. But on focus, helper text is shown over error text.
                val error = validationError
                if (error != null && error.isNotEmpty() && hasInteracted && !isFocused) {
                    BisqGap.VQuarter()
                    BisqText.smallLight(
                        text = error,
                        modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                        color = BisqTheme.colors.danger
                    )
                } else if (helperText.isNotEmpty()) {
                    BisqGap.VQuarter()
                    BisqText.smallLightGrey(
                        text = helperText,
                        modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                    )
                }
            }
        }
    )
}

fun processText(
    newValue: String,
    oldValue: String,
    valuePrefix: String?,
    valueSuffix: String?,
    maxLength: Int,
    numberWithTwoDecimals: Boolean,
    decimalSeparator: String,
    decimalLoosePattern: Regex,
): String {
    var cleanValue = newValue
    if (valuePrefix != null && cleanValue.startsWith(valuePrefix)) {
        cleanValue = cleanValue.removePrefix(valuePrefix)
    }
    if (valueSuffix != null && cleanValue.endsWith(valueSuffix)) {
        cleanValue = cleanValue.removeSuffix(valueSuffix)
    }
    if (maxLength != 0 && cleanValue.length > maxLength) {
        return oldValue
    }
    if (numberWithTwoDecimals) {
        val separator = decimalSeparator
        val loosePattern = decimalLoosePattern

        if (!loosePattern.matches(cleanValue)) {
            return oldValue
        }
        val parts = cleanValue.split(separator)
        val integerPart = parts[0]
        val decimalPart = if (parts.size == 2) parts[1] else ""

        val trimmedValue = when {
            parts.size == 2 && decimalPart.length > 2 -> {
                "$integerPart$separator${decimalPart.take(2)}"
            }

            else -> cleanValue // let the user keep typing normally
        }
        return trimmedValue
    } else {
        return cleanValue
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_Empty() {
    var text by remember { mutableStateOf("") }
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Label",
                value = text,
                onValueChange = { newText, _ -> text = newText },
                placeholder = "Placeholder text...",
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_Search() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Label",
                placeholder = "Search text",
                onValueChange = { _, _ -> },
                isSearch = true
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_TextArea() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Your Notes",
                value = "This is a multi-line text area.\nIt can contain multiple lines of text, and will grow up to the maxLines limit.",
                onValueChange = { _, _ -> },
                isTextArea = true,
                helperText = "Maximum 4 lines",
                maxLines = 4
            )
        }
    }
}


@Preview
@Composable
private fun BisqTextFieldPreview_WithValue() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Label",
                value = "Some value entered by the user",
                onValueChange = { _, _ -> }
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_WithHelperText() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Label",
                value = "A valid value",
                onValueChange = { _, _ -> },
                helperText = "This is some helpful text."
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_WithError() {
    BisqTheme.Preview {
        // To preview the error state, we must simulate interaction.
        // The error text will not show in a static preview unless hasInteracted=true.
        // This preview provides the validation logic; the red text will appear in an interactive preview.
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Field with Validation",
                value = "invalid",
                onValueChange = { _, _ -> },
                validation = { "This field has an error." }
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_Disabled() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Disabled Field",
                value = "You can't edit this",
                onValueChange = { _, _ -> },
                disabled = true
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_WithSuffixes() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Amount",
                value = "1,250.75",
                onValueChange = { _, _ -> },
                leftSuffix = {
                    BisqText.baseRegular(
                        text = "₿",
                        color = BisqTheme.colors.light_grey20
                    )
                },
                rightSuffix = {
                    BisqText.baseRegular(
                        text = "BTC",
                        color = BisqTheme.colors.light_grey20
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun BisqTextFieldPreview_LabelWithSuffix() {
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(16.dp)) {
            BisqTextField(
                label = "Amount",
                value = "100.00",
                onValueChange = { _, _ -> },
                labelRightSuffix = { BisqText.smallLightGrey(text = "Optional") }
            )
        }
    }
}
