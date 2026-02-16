package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BisqFiatInputField(
    value: String,
    currency: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    smallFont: Boolean = false,
    maxLength: Int = 8,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
                .background(color = BisqTheme.colors.dark_grey40)
                .border(
                    width = 1.dp,
                    color = if (isError) BisqTheme.colors.danger else BisqTheme.colors.transparent,
                    shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf),
                ),
    ) {
        BisqTextFieldV0(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= maxLength) {
                    onValueChange(newValue)
                }
            },
            enabled = enabled,
            textStyle =
                TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Light,
                ),
            suffix = {
                if (smallFont) {
                    BisqText.H6Regular(text = currency)
                } else {
                    BisqText.H5Regular(text = currency)
                }
            },
            isError = isError,
            bottomMessage = errorMessage,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
}

@Preview
@Composable
private fun BisqFiatInputField_DefaultPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("500") }
            BisqFiatInputField(
                value = textState,
                currency = "USD",
                onValueChange = { textState = it },
            )
        }
    }
}

@Preview
@Composable
private fun BisqFiatInputField_EmptyPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("") }
            BisqFiatInputField(
                value = textState,
                currency = "EUR",
                onValueChange = { textState = it },
            )
        }
    }
}

@Preview
@Composable
private fun BisqFiatInputField_ErrorPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("999999") }
            BisqFiatInputField(
                value = textState,
                currency = "USD",
                onValueChange = { textState = it },
                isError = true,
                errorMessage = "Amount exceeds maximum limit",
            )
        }
    }
}

@Preview
@Composable
private fun BisqFiatInputField_DisabledPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("1000") }
            BisqFiatInputField(
                value = textState,
                currency = "GBP",
                onValueChange = { textState = it },
                enabled = false,
            )
        }
    }
}

@Preview
@Composable
private fun BisqFiatInputField_SmallFontPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("250") }
            BisqFiatInputField(
                value = textState,
                currency = "USD",
                onValueChange = { textState = it },
                smallFont = true,
            )
        }
    }
}

@Preview
@Composable
private fun BisqFiatInputField_LargeValuePreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("99999999") }
            BisqFiatInputField(
                value = textState,
                currency = "JPY",
                onValueChange = { textState = it },
                smallFont = true,
            )
        }
    }
}

@Preview
@Composable
private fun BisqFiatInputField_LongCurrencyPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("100") }
            BisqFiatInputField(
                value = textState,
                currency = "BTC",
                onValueChange = { textState = it },
            )
        }
    }
}
