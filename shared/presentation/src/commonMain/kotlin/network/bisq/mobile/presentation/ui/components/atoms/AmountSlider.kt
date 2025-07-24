package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun AmountSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float = 0f,
    max: Float = 1f,
    leftMarker: Float? = null,
    rightMarker: Float? = null,
    modifier: Modifier = Modifier,
) {
    require(min < max) { "min must be less than max" }

    // Normalize a real value to [0f..1f] range
    fun Float.normalized(): Float = ((this - min) / (max - min)).coerceIn(0f, 1f)

    val normalizedValue = value.normalized()
    val normalizedLeftMarker = leftMarker?.normalized()
    val normalizedRightMarker = rightMarker?.normalized()

    val dragState = rememberDraggableState { delta ->
        val range = max - min
        val deltaValue = (delta / 1000f) * range
        val newValue = (value + deltaValue).coerceIn(min, max)
        onValueChange(newValue)
    }

    Box(
        modifier = modifier.fillMaxWidth().draggable(
                orientation = Orientation.Horizontal, state = dragState
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize().height(40.dp)) {
            val thumbRadius = 12.dp.toPx()
            val width = size.width
            val centerY = size.height / 2
            val trackHeight = 3.dp.toPx()  // We use 2 px in Bisq Easy but seems to small here

            val thumbPos = normalizedValue * width
            val leftPos = (normalizedLeftMarker ?: 0f) * width
            val rightPos = (normalizedRightMarker ?: 1f) * width

            // Track
            drawLine(
                color = BisqTheme.colors.mid_grey10,  // We use mid_grey20 in Bisq Easy but seems to bright here
                start = Offset(thumbRadius / 2, centerY),
                end = Offset(width - thumbRadius / 2, centerY),
                strokeWidth = trackHeight
            )

            // Marker range
            if (leftMarker != null || rightMarker != null) {
                drawLine(
                    color = BisqTheme.colors.primary,
                    start = Offset(leftPos, centerY),
                    end = Offset(rightPos, centerY),
                    strokeWidth = trackHeight
                )
            }

            // Thumb
            drawCircle(
                color = BisqTheme.colors.primary, radius = thumbRadius, center = Offset(
                    thumbPos.coerceIn(thumbRadius / 2, width - thumbRadius / 2), centerY
                )
            )
        }
    }
}
