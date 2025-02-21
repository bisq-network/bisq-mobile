package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.runtime.Composable

@Composable
fun InfoRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
    style: InfoBoxStyle = InfoBoxStyle.Style1,
) {
    InfoRowContainer {
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