package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    editable: Boolean,
    onValueChange: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = BisqTheme.colors.grey1,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        TextField(
            value = value,
            enabled = editable,
            onValueChange = onValueChange,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = BisqTheme.colors.secondaryDisabled,
                disabledTextColor = BisqTheme.colors.light5,
                focusedTextColor = BisqTheme.colors.light3,
                unfocusedTextColor = BisqTheme.colors.secondaryHover,
                unfocusedIndicatorColor = BisqTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                focusedContainerColor = BisqTheme.colors.secondary,
                cursorColor = Color.Blue,
                unfocusedContainerColor = BisqTheme.colors.secondary
            ),
//            fontSize = 14.sp,
//            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.dark1, RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
    }
}