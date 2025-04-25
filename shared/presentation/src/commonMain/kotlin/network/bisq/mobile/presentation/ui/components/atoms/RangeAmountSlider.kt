package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RangeAmountSlider(
    minRangeValue: MutableStateFlow<Float>,
    onMinRangeValueChange: (Float) -> Unit,
    maxRangeValue: MutableStateFlow<Float>,
    onMaxRangeValueChange: (Float) -> Unit,
    maxValue: StateFlow<Float?> = MutableStateFlow(null),
    leftMarkerValue: StateFlow<Float?> = MutableStateFlow(null),
    rightMarkerValue: StateFlow<Float?> = MutableStateFlow(null),
) {
    val minState by minRangeValue.collectAsState()

    val maxState by maxRangeValue.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        AmountSlider(
            value = minRangeValue,
            maxValue = maxValue,
            leftMarkerValue = leftMarkerValue,
            rightMarkerValue = rightMarkerValue,
            onValueChange = { value ->
                minRangeValue.value = value
                if (value > maxState) {
                    maxRangeValue.value = value // shift max along with min
                    onMaxRangeValueChange(value)
                }
                onMinRangeValueChange(value)
            }
        )

        AmountSlider(
            value = maxRangeValue,
            maxValue = maxValue,
            leftMarkerValue = leftMarkerValue,
            rightMarkerValue = rightMarkerValue,
            onValueChange = { value ->
                maxRangeValue.value = value
                if (value < minState) {
                    minRangeValue.value = value // shift min along with max
                    onMinRangeValueChange(value)
                }
                onMaxRangeValueChange(value)
            }
        )
    }
}
