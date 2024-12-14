package network.bisq.mobile.presentation.ui.components.molecules.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BreadcrumbNavigation(
    path: List<MenuItem>,
    onBreadcrumbClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        path.forEachIndexed { index, menuItem ->
            Text(
                text = menuItem.label,
                style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.grey1),
                modifier = Modifier.clickable { onBreadcrumbClick(index) }
            )
            if (index != path.lastIndex) {
                Text(" > ", color = BisqTheme.colors.grey1) // Separator
            }
        }
    }
}