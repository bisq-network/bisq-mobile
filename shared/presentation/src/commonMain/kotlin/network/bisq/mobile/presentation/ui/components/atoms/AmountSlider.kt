package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun AmountSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    max: Float? = null,
    leftMarker: Float? = null,
    rightMarker: Float? = null,
    modifier: Modifier = Modifier,
) {

    // max and rightMarker cannot be > 1
    // leftMarker cannot be < 0

    val dragState = rememberDraggableState { delta ->
        val newValue = (value + delta / 1000f).coerceIn(0f, max ?: 1f).coerceIn(0f, 1f)
        onValueChange(newValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thumbRadius = 12.dp.toPx()
            val width = size.width
            val centerY = size.height / 2
            val trackHeight = 3.dp.toPx()  // We use 2 px in Bisq 2 but seems to small here

            val maxPos = (max ?: 1f).coerceIn(0f, 1f) * width
            val thumbPos = value.coerceIn(0f, 1f) * width

            // Track
            drawLine(
                color = BisqTheme.colors.mid_grey10, // We use mid_grey20 in Bisq 2 but seems to bright here
                start = Offset(thumbRadius / 2, centerY),
                end = Offset(width - thumbRadius / 2, centerY),
                strokeWidth = trackHeight
            )

            // Marker range
            drawLine(
                color = BisqTheme.colors.primary,
                start = Offset((leftMarker ?: 0f).coerceIn(0f, 1f) * width, centerY),
                end = Offset(((max ?: rightMarker) ?: 0f).coerceIn(0f, 1f) * width, centerY),
                strokeWidth = trackHeight
            )

            // Thumb
            drawCircle(
                color = BisqTheme.colors.primary,
                radius = thumbRadius,
                center = Offset(thumbPos.coerceAtMost(maxPos), centerY)
            )
        }
    }
}

