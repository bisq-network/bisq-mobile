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
import network.bisq.mobile.presentation.ui.components.context.LocalAnimationsEnabled
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
) {
    var hasInteracted by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val animatedLineProgress by animateFloatAsState(
        targetValue = if (isFocused && enableAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "BottomBorderAnimation"
    )

    val focusManager = LocalFocusManager.current

    val imeAction = when {
        isSearch -> ImeAction.Search
        isTextArea -> ImeAction.Next
        else -> ImeAction.Done
    }

    val finalValue = buildString {
        valuePrefix?.let { append(it) }
        append(value)
        valueSuffix?.let { append(it) }
    }

    val dangerColor = BisqTheme.colors.danger
    val grey2Color = BisqTheme.colors.mid_grey20
    val finalIndicatorColor = when {
        !validationError.isNullOrEmpty() && hasInteracted && enableAnimation -> dangerColor
        !enableAnimation && isFocused -> indicatorColor
        else -> grey2Color
    }

    val secondaryColor = BisqTheme.colors.secondary
    val secondaryHoverColor = BisqTheme.colors.secondaryHover
    val secondaryDisabledColor = BisqTheme.colors.secondaryDisabled
    val finalBackgroundColor by remember(disabled, isFocused) {
        derivedStateOf {
            when {
                disabled -> secondaryDisabledColor
                isFocused -> secondaryHoverColor
                else -> secondaryColor
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
        validationError = validation?.invoke(value)
    }

    val finalBorderRadius by remember(isFocused) {
        mutableStateOf(
            if (isFocused) {
                0.dp
            } else {
                6.dp
            }
        )
    }

    val decimalSeparator = remember { getDecimalSeparator().toString() }
    val decimalLoosePattern = remember(decimalSeparator) {
        Regex("^[-]?\\d*(${Regex.escape(decimalSeparator)}\\d{0,})?$")
    }

    Column(modifier = modifier) {
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
                .clip(shape = RoundedCornerShape(topStart = finalBorderRadius, topEnd = finalBorderRadius))
                .background(color = finalBackgroundColor)
                .drawBehind {
                    if (!isSearch) {
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
        ) {
            BasicTextField(value = finalValue,
                onValueChange = {
                    val processedValue = processText(
                        it,
                        valuePrefix,
                        valueSuffix,
                        maxLength,
                        numberWithTwoDecimals,
                        decimalSeparator,
                        decimalLoosePattern
                    )
                    if (processedValue == value) return@BasicTextField
                    validationError = validation?.invoke(processedValue)
                    onValueChange?.invoke(
                        processedValue, validationError.isNullOrEmpty()
                    )
                },
                modifier = Modifier.padding(paddingValues).fillMaxWidth().onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!focusState.isFocused) {
                        if (value.length > 0) hasInteracted = true
                        validationError = validation?.invoke(value)
                    }
                },
                singleLine = !isTextArea,
                maxLines = maxLines,
                minLines = minLines,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType, imeAction = imeAction
                ),
                cursorBrush = SolidColor(BisqTheme.colors.primary),
                enabled = !disabled,
                textStyle = finalTextStyle,
                decorationBox = { innerTextField ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (leftSuffix != null) {
                            leftSuffix()
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = textFieldAlignment,
                        ) {
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
                                onValueChange?.invoke(it, validationError.isNullOrEmpty())
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
                    }
                })
        }
        // Error text has priority over help field. But on focus, helper text is shown over error text.
        if (validationError?.isNotEmpty() == true && hasInteracted && !isFocused) {
            BisqGap.VQuarter()
            BisqText.smallLight(
                text = validationError!!,
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


fun processText(
    value: String,
    valuePrefix: String?,
    valueSuffix: String?,
    maxLength: Int,
    numberWithTwoDecimals: Boolean,
    decimalSeparator: String,
    decimalLoosePattern: Regex,
): String {
    var cleanValue = value
    if (valuePrefix != null && cleanValue.startsWith(valuePrefix)) {
        cleanValue = cleanValue.removePrefix(valuePrefix)
    }
    if (valueSuffix != null && cleanValue.endsWith(valueSuffix)) {
        cleanValue = cleanValue.removeSuffix(valueSuffix)
    }
    if (maxLength != 0 && cleanValue.length > maxLength) {
        return cleanValue
    }
    if (numberWithTwoDecimals) {
        val separator = decimalSeparator
        val loosePattern = decimalLoosePattern

        if (!loosePattern.matches(cleanValue)) {
            return cleanValue
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