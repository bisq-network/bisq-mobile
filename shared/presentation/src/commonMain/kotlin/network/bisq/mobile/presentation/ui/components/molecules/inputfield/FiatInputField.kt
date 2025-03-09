package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun FiatInputField(
    text: String,
    onValueChanged: (String) -> Unit = {},
    label: String = "",
    enabled: Boolean = true,
    currency: String,
    paddingValues: PaddingValues = PaddingValues(all = 0.dp),
    indicatorColor: Color = BisqTheme.colors.primary,
) {
    var isFocused by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .clip(shape = RoundedCornerShape(6.dp))
            .background(color = BisqTheme.colors.dark4)
            .drawBehind {
                if (isFocused) {
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
            value = text,
            onValueChange = onValueChanged,
            enabled = enabled,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 32.sp,
                textAlign = TextAlign.End,
                textDecoration = TextDecoration.None
            ),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            cursorBrush = SolidColor(BisqTheme.colors.primary),
            decorationBox = { innerTextField ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (label.isNotEmpty()) {
                        BisqText.h5RegularGrey(
                            text = label,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        innerTextField()
                    }

                    BisqText.h5Regular(currency)
                }
            }
        )
    }
}