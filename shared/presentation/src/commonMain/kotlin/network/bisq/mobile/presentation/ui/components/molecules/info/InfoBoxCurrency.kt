package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.ui.components.molecules.AmountWithCurrency

@Composable
fun InfoBoxCurrency(
    label: String,
    value: String,
    rightAlign: Boolean = false,
) {
    InfoBox(
        label = label,
        rightAlign = rightAlign,
        valueComposable = {
            AmountWithCurrency(value)
        }
    )
}