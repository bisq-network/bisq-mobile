package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RangeAmountSlider(
    minRangeInitialValue: Float,
    onMinRangeValueChange: (Float) -> Unit,
    maxRangeInitialValue: Float,
    onMaxRangeValueChange: (Float) -> Unit,
    maxValue: StateFlow<Float?> = MutableStateFlow(null),
    leftMarkerValue: StateFlow<Float?> = MutableStateFlow(null),
    rightMarkerValue: StateFlow<Float?> = MutableStateFlow(null),
) {
    val minMutableValue = remember { MutableStateFlow(minRangeInitialValue) }
    val minMutableVal by minMutableValue.collectAsState()

    val maxMutableValue = remember { MutableStateFlow(maxRangeInitialValue) }
    val maxMutableVal by maxMutableValue.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        AmountSlider(
            value = minMutableValue,
            maxValue = maxValue,
            leftMarkerValue = leftMarkerValue,
            rightMarkerValue = rightMarkerValue,
            onValueChange = { newMin ->
                if (newMin > maxMutableVal) {
                    minMutableValue.value = newMin
                    maxMutableValue.value = newMin // shift max along with min
                    onMaxRangeValueChange(maxMutableVal)
                } else {
                    minMutableValue.value = newMin
                }
                onMinRangeValueChange(newMin)
            }
        )

        AmountSlider(
            value = maxMutableValue,
            maxValue = maxValue,
            leftMarkerValue = leftMarkerValue,
            rightMarkerValue = rightMarkerValue,
            onValueChange = { newMax ->
                if (newMax < minMutableVal) {
                    maxMutableValue.value = newMax
                    minMutableValue.value = newMax // shift min along with max
                    onMinRangeValueChange(minMutableVal)
                } else {
                    maxMutableValue.value = newMax
                }
                onMaxRangeValueChange(newMax)
            }
        )
    }
}
