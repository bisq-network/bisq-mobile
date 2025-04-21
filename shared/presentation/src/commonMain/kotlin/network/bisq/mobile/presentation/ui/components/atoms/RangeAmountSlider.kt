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
    val minStateFlow = remember { MutableStateFlow(minRangeInitialValue) }
    val minState by minStateFlow.collectAsState()

    val maxStateFlow = remember { MutableStateFlow(maxRangeInitialValue) }
    val maxState by maxStateFlow.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        AmountSlider(
            value = minStateFlow,
            maxValue = maxValue,
            leftMarkerValue = leftMarkerValue,
            rightMarkerValue = rightMarkerValue,
            onValueChange = { value ->
                minStateFlow.value = value
                if (value > maxState) {
                    maxStateFlow.value = value // shift max along with min
                    onMaxRangeValueChange(value)
                }
                onMinRangeValueChange(value)
            }
        )

        AmountSlider(
            value = maxStateFlow,
            maxValue = maxValue,
            leftMarkerValue = leftMarkerValue,
            rightMarkerValue = rightMarkerValue,
            onValueChange = { value ->
                maxStateFlow.value = value
                if (value < minState) {
                    minStateFlow.value = value // shift min along with max
                    onMinRangeValueChange(value)
                }
                onMaxRangeValueChange(value)
            }
        )
    }
}
