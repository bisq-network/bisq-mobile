package network.bisq.mobile.presentation.common.ui.components.molecules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton

@Composable
fun InfoBoxRow(
    label: String,
    value: String,
    fullValueToCopy: String = value,
    showCopy: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            InfoBox(label = label, value = value)
        }

        if (showCopy) {
            CopyIconButton(value = fullValueToCopy, showToast = true)
        }
    }
}
