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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqTextField(
    label: String = "",
    value: String = "",
    onValueChanged: (String) -> Unit = {},
    placeholder: String = "",
    labelRightSuffix: (@Composable () -> Unit)? = null,
    leftSuffix: (@Composable () -> Unit)? = null,
    rightSuffix: (@Composable () -> Unit)? = null,
    rightSuffixModifier: Modifier = Modifier.width(50.dp),
    isSearch: Boolean = false,
    helperText: String = "",
    errorText: String = "",
    indicatorColor: Color = BisqTheme.colors.primary,
    isTextArea: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(all = 12.dp),
    disabled: Boolean = false,
    color: Color = BisqTheme.colors.light2,
    showCopy: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val imeAction = when {
        isSearch -> ImeAction.Search
        isTextArea -> ImeAction.Next
        else -> ImeAction.Done
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
                    color = BisqTheme.colors.light2,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
                if (labelRightSuffix != null) {
                    labelRightSuffix()
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(6.dp))
                .background(color = BisqTheme.colors.secondary)
                .drawBehind {
                    if (!isSearch && isFocused) {
                        drawLine(
                            color = indicatorColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                }
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                singleLine = !isTextArea,
                maxLines = if (isTextArea) 4 else 1,
                textStyle = TextStyle(
                    color = color,
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.None
                ),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                cursorBrush = SolidColor(BisqTheme.colors.primary),
                enabled = !disabled,
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
                                BisqText.largeLight(
                                    text = placeholder,
                                    color = BisqTheme.colors.grey2
                                )
                            }
                            innerTextField()
                        }


                        if (showCopy) {
                            CopyIconButton(value)
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
        if (errorText.isNotEmpty()) {
            BisqText.smallRegular(
                text = errorText,
                modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                color = BisqTheme.colors.danger
            )
        } else if (helperText.isNotEmpty()) {
            BisqText.smallRegular(
                text = helperText,
                modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 4.dp),
                color = BisqTheme.colors.grey1
            )
        }
    }
}