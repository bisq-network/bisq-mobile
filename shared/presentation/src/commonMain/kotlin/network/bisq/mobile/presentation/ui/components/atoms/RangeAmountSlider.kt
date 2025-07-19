package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun RangeAmountSlider(
    minRangeValue: Float,
    onMinRangeValueChange: (Float) -> Unit,
    maxRangeValue: Float,
    onMaxRangeValueChange: (Float) -> Unit,
    maxValue: Float? = null,
    leftMarkerValue: Float? = null,
    rightMarkerValue: Float? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        AmountSlider(
            value = minRangeValue,
            max= maxValue ?: 1f,
            leftMarker= leftMarkerValue,
            rightMarker= rightMarkerValue,
            onValueChange = { value ->
                if (value > maxRangeValue) {
                    onMaxRangeValueChange(value)
                }
                onMinRangeValueChange(value)
            }
        )

        AmountSlider(
            value = maxRangeValue,
            max= maxValue ?: 1f,
            leftMarker= leftMarkerValue,
            rightMarker= rightMarkerValue,
            onValueChange = { value ->
                if (value < minRangeValue) {
                    onMinRangeValueChange(value)
                }
                onMaxRangeValueChange(value)
            }
        )
    }
}
