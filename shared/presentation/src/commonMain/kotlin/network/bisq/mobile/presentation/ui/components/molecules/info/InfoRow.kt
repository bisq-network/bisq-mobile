package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun InfoRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
    style: InfoBoxStyle = InfoBoxStyle.Style1,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        InfoBox(
            label = label1,
            value = value1,
            valueType = valueType,
            style = style,
        )
        InfoBox(
            label = label2,
            value = value2,
            valueType = valueType,
            rightAlign = true,
            style = style,
        )
    }
}