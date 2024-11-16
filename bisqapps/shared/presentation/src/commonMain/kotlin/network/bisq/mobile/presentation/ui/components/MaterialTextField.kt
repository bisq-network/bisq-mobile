package network.bisq.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.*

@Composable
fun MaterialTextField(text: String, placeholder: String = "", onValueChanged: (String) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(shape = RoundedCornerShape(6.dp))
            .background(color = BisqTheme.colors.secondary)
    ) {
        TextField(
            value = text,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clickable { isFocused = true }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = TextStyle(fontSize = 22.sp),
            onValueChange = onValueChanged,
            colors = TextFieldDefaults.colors(
                focusedTextColor = BisqTheme.colors.light3,
                unfocusedTextColor = BisqTheme.colors.secondaryHover,
                unfocusedIndicatorColor = BisqTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                focusedContainerColor = BisqTheme.colors.secondary,
                cursorColor = Color.Blue,
                unfocusedContainerColor = BisqTheme.colors.secondary
            ),
            placeholder = {
                BisqText.h5Regular(
                    text = placeholder,
                    color = BisqTheme.colors.secondaryHover,
                )
            }
        )
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(BisqTheme.colors.primary)
            )
        }
    }
}