package network.bisq.mobile.presentation.ui.components.atoms

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.domain.getDecimalSeparator
import network.bisq.mobile.presentation.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.ui.components.atoms.button.PasteIconButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

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
    isSearch: Boolean = false,
    helperText: String = "",
    indicatorColor: Color = BisqTheme.colors.primary,
    isTextArea: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    paddingValues: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPadding),
    maxLength: Int = 0,
    disabled: Boolean = false,
    color: Color = BisqTheme.colors.light_grey20,
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
) {
    var hasInteracted by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    val imeAction = when {
        isSearch -> ImeAction.Search
        isTextArea -> ImeAction.Next
        else -> ImeAction.Done
    }

    var finalValue = ""
    finalValue = if (valuePrefix != null) valuePrefix + value else value
    finalValue = if (valueSuffix != null) value + valueSuffix else value

    val dangerColor = BisqTheme.colors.danger
    val grey2Color = BisqTheme.colors.mid_grey20
    val finalIndicatorColor by remember(validationError, isFocused, hasInteracted) {
        mutableStateOf(
            if (validationError == null || validationError?.isEmpty() == true || !hasInteracted)
                if (isFocused) indicatorColor else grey2Color
            else
                dangerColor
        )
    }

    val secondaryColor = BisqTheme.colors.secondary
    val secondaryHoverColor = BisqTheme.colors.secondaryHover
    val secondaryDisabledColor = BisqTheme.colors.secondaryDisabled
    val finalBackgroundColor by remember(disabled, isFocused) {
        mutableStateOf(
            if (disabled) {
                secondaryDisabledColor
            } else if (isFocused) {
                secondaryHoverColor
            } else {
                secondaryColor
            }
        )
    }

    val whiteColor = BisqTheme.colors.white
    val finalLabelColor by remember(disabled) {
        mutableStateOf(
            if (disabled) {
                grey2Color
            } else {
                whiteColor
            }
        )
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
        validationError = validation?.invoke(value)
    }

    Column(
        modifier = modifier
    ) {
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
                if (labelRightSuffix != null) {
                    labelRightSuffix()
                }
            }

            BisqGap.VQuarter()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(6.dp))
                .background(color = finalBackgroundColor)
                .drawBehind {
                    if (!isSearch) {
                        drawLine(
                            color = finalIndicatorColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                }
        ) {
            BasicTextField(
                value = finalValue,
                onValueChange = {
                    var cleanValue = it
                    if (valuePrefix != null && cleanValue.startsWith(valuePrefix)) {
                        cleanValue = cleanValue.removePrefix(valuePrefix)
                    }
                    if (valueSuffix != null && cleanValue.endsWith(valueSuffix)) {
                        cleanValue = cleanValue.removeSuffix(valueSuffix)
                    }
                    if (maxLength != 0 && cleanValue.length > maxLength) {
                        return@BasicTextField
                    }
                    if (numberWithTwoDecimals) {
                        val separator = getDecimalSeparator().toString()
                        val escapedSeparator = Regex.escape(separator)
                        val loosePattern = Regex("^\\d*${escapedSeparator}?\\d*$")

                        if (loosePattern.matches(cleanValue)) {
                            val trimmedValue = if (cleanValue.contains(separator)) {
                                val parts = cleanValue.split(separator)
                                if (parts.size == 2) {
                                    val integer = parts[0]
                                    val decimals = parts[1].take(2) // Trim to 2 decimal digits
                                    "$integer$separator$decimals"
                                } else {
                                    cleanValue // malformed (multiple separators), leave as-is
                                }
                            } else {
                                cleanValue
                            }

                            validationError = validation?.invoke(trimmedValue)
                            onValueChange?.invoke(
                                trimmedValue,
                                validationError == null || validationError?.isEmpty() == true
                            )
                        }
                    } else {
                        validationError = validation?.invoke(cleanValue)
                        onValueChange?.invoke(cleanValue, validationError == null || validationError?.isEmpty() == true)
                    }
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            if (value.length > 0)
                                hasInteracted = true
                            validationError = validation?.invoke(value)
                        }
                    },
                singleLine = !isTextArea,
                maxLines = if (isTextArea) 4 else 1,
                minLines = if (isTextArea) 2 else 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                cursorBrush = SolidColor(BisqTheme.colors.primary),
                enabled = !disabled,
                textStyle = textStyle,
                decorationBox = { innerTextField ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (leftSuffix != null) {
                            leftSuffix()
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                BisqText.largeLightGrey(placeholder)
                            }
                            innerTextField()
                        }

                        if (showCopy) {
                            CopyIconButton(value)
                        }

                        if (showPaste) {
                            PasteIconButton(onPaste = {
                                validationError = validation?.invoke(it)
                                onValueChange?.invoke(it, validationError == null)
                                hasInteracted = true
                            })
                        }

                        if (rightSuffix != null) {
                            Box(modifier = rightSuffixModifier) {
                                rightSuffix()
                            }
                        }
                    }
                }
            )
        }
        // Error text has priority over help field
        if (validationError?.isNotEmpty() == true && hasInteracted) {
            BisqGap.VQuarter()
            BisqText.smallRegular(
                text = validationError!!,
                modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                color = BisqTheme.colors.danger
            )
        } else if (helperText.isNotEmpty()) {
            BisqGap.VQuarter()
            BisqText.smallRegularGrey(
                text = helperText,
                modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
            )
        }
    }
}