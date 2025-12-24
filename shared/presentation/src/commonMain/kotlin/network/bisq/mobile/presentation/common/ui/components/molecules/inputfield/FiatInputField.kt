// TODO: remove and fix the issue
@file:Suppress("ktlint:compose:lambda-param-in-effect")

package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun FiatInputField(
    text: String,
    currency: String,
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
    paddingValues: PaddingValues = PaddingValues(all = 0.dp),
    textAlign: TextAlign = TextAlign.End,
    validation: ((String) -> String?)? = null,
    smallFont: Boolean = false,
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
                .background(color = BisqTheme.colors.dark_grey40)
                .border(
                    1.dp,
                    if (validationError == null) BisqTheme.colors.transparent else BisqTheme.colors.danger,
                    RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf),
                ),
    ) {
        val fontSize =
            if (smallFont) {
                if (text.length < 6) {
                    24.sp
                } else if (text.length < 8) {
                    20.sp
                } else {
                    16.sp
                }
            } else {
                32.sp
            }
        BisqTextField(
            value = text,
            onValueChange = { newValue, isValid ->
                onValueChange(newValue)
            },
            keyboardType = KeyboardType.Number,
            rightSuffix = {
                if (smallFont) {
                    BisqText.H6Regular(currency, modifier = Modifier.offset(y = (-2).dp))
                } else {
                    BisqText.H5Regular(currency, modifier = Modifier.offset(y = (-2).dp))
                }
            },
            indicatorColor = BisqTheme.colors.transparent,
            textStyle =
                TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    textAlign = textAlign,
                    fontWeight = FontWeight.Light,
                    textDecoration = TextDecoration.None,
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
