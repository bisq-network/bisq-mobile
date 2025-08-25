package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun InfoBoxSats(
    label: String,
    value: String,
    textStyle: TextStyle = BisqTheme.typography.h6Regular,
    rightAlign: Boolean = false,
) {
    InfoBox(
        label = label,
        rightAlign = rightAlign,
        valueComposable = {
            BtcSatsText(value, textStyle = textStyle)
        }
    )
}