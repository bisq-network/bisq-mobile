package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
) {
    var validationError: String? by remember { mutableStateOf(null) }
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
        BisqTextField(
            value = text,
            onValueChange = { newValue, isValid ->
                onValueChanged(newValue)
            },
            keyboardType = KeyboardType.Number,
            rightSuffix = {
                BisqText.h5Regular(currency, modifier = Modifier.offset(y = (-2).dp))
            },
            indicatorColor = BisqTheme.colors.transparent,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 32.sp,
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
            disabled = !enabled,
        )
    }
}