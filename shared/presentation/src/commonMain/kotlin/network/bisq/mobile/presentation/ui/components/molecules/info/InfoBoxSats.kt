package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.FontSize

@Composable
fun InfoBoxSats(
    label: String,
    value: String,
    fontSize: FontSize = FontSize.H6,
    rightAlign: Boolean = false,
) {
    InfoBox(
        label = label,
        rightAlign = rightAlign,
        valueComposable = {
            BtcSatsText(value, fontSize = fontSize)
        }
    )
}