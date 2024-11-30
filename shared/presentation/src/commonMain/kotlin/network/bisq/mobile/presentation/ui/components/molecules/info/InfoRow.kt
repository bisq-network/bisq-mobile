package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.text.InfoBox
import network.bisq.mobile.presentation.ui.components.atoms.text.InfoBoxValueType
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun InfoRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        InfoBox(
            label = label1,
            value = value1,
            valueType = valueType
        )
        InfoBox(
            label = label2,
            value = value2,
            valueType = valueType,
            rightAlign = true
        )
    }
}