package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String?,
    labelRightSuffix: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    ) {
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BisqText.baseRegular(
                    text = label,
                    color = BisqTheme.colors.light2,
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
        ) {
            TextField(
                value = value,
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
                    if (placeholder != null) {
                        BisqText.h5Regular(
                            text = placeholder,
                            color = BisqTheme.colors.secondaryHover,
                            )
                    }
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
}