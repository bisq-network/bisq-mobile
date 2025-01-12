package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon

@Composable
fun CloseIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(20.dp)
) {
    IconButton(onClick = onClick, modifier = modifier) {
        CloseIcon()
    }
}