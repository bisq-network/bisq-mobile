package network.bisq.mobile.presentation.ui.components.molecules.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun SettingsButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BisqTheme.colors.grey5)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.light1 , fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ">",
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.light1, fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
    }
}