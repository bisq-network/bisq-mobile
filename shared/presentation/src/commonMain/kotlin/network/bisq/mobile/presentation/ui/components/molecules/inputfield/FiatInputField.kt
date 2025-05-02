package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun FiatInputField(
    text: String,
    onValueChanged: (String) -> Unit = {},
    enabled: Boolean = true,
    currency: String,
    paddingValues: PaddingValues = PaddingValues(all = 0.dp),
    textAlign: TextAlign = TextAlign.End,
    validation: ((String) -> String?)? = null,
    smallFont: Boolean = false
) {
    var validationError: String? by remember { mutableStateOf(null) }
    val maxLength = 8

    // This triggers double validation, when user types value in the field
    // But is necessary to re-validate when value changes from outside.
    LaunchedEffect(text) {
        if (validation != null) {
            validationError = validation(text)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
            .background(color = BisqTheme.colors.dark_grey40)
            .border(
                1.dp,
                if (validationError == null) BisqTheme.colors.transparent else BisqTheme.colors.danger,
                RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf)
            )
    ) {
        val fontSize = if (smallFont) {
            if (text.length < 6)
                24.sp
            else if (text.length < 8)
                20.sp
            else
                16.sp
        } else {
            32.sp
        }
        BisqTextField(
            value = text,
            onValueChange = { newValue, isValid ->
                onValueChanged(newValue)
            },
            keyboardType = KeyboardType.Number,
            rightSuffix = {
                if (smallFont)
                    BisqText.h6Regular(currency, modifier = Modifier.offset(y = (-2).dp))
                else
                    BisqText.h5Regular(currency, modifier = Modifier.offset(y = (-2).dp))
            },
            indicatorColor = BisqTheme.colors.transparent,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = fontSize,
                textAlign = textAlign,
                textDecoration = TextDecoration.None
            ),
            validation = {
                if (validation != null) {
                    validationError = validation(it)
                    return@BisqTextField null
                }
                return@BisqTextField null
            },
            maxLength = maxLength,
            disabled = !enabled,
        )
    }
}