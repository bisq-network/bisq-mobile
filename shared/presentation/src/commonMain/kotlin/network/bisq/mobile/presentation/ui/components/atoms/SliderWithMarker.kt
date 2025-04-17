package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun SliderWithMarker(
    initialValue: Float,
    onValueChange: (Float) -> Unit,
    leftMarkerQuoteSideValue: StateFlow<Float>,
    rightMarkerQuoteSideValue: StateFlow<Float>,
    modifier: Modifier = Modifier,
) {
    val leftMarker by leftMarkerQuoteSideValue.collectAsState()
    val rightMarker by rightMarkerQuoteSideValue.collectAsState()

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .height(48.dp)
                .padding(horizontal = 8.dp),
        ) {
            val trackHeight = 3.dp.toPx()  // We use 2 px in Bisq 2 but seems to small here
            val centerY = size.height / 2

            // Track background
            drawLine(
                color = BisqTheme.colors.mid_grey10, // We use mid_grey20 in Bisq 2 but seems to bright here
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = trackHeight
            )

            // Track marker
            drawLine(
                color = BisqTheme.colors.primary,
                start = Offset(leftMarker * size.width, centerY),
                end = Offset(rightMarker * size.width, centerY),
                strokeWidth = trackHeight
            )
        }

        // We set track color to transparent
        BisqSlider(
            initialValue = initialValue,
            onValueChange = onValueChange,
            colors = SliderColors(
                thumbColor = BisqTheme.colors.primary,
                activeTrackColor = Color.Transparent,
                activeTickColor = Color.Unspecified,
                inactiveTrackColor = Color.Transparent,
                inactiveTickColor = Color.Unspecified,
                disabledThumbColor = Color.Unspecified,
                disabledActiveTrackColor = Color.Unspecified,
                disabledActiveTickColor = Color.Unspecified,
                disabledInactiveTrackColor = Color.Unspecified,
                disabledInactiveTickColor = Color.Unspecified
            )
        )
    }
}
