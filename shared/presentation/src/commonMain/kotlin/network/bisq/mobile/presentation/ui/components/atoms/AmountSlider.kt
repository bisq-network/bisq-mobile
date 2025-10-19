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
    // Gracefully handle a degenerate range (min >= max): render at fixed position and disable dragging
    val hasRange = max > min
    val range = if (hasRange) (max - min) else 1f

    // Normalize a real value to [0f..1f] range
    fun Float.normalized(): Float = if (hasRange) ((this - min) / range).coerceIn(0f, 1f) else 0f

    val normalizedValue = value.normalized()
    val normalizedLeftMarker = leftMarker?.normalized()
    val normalizedRightMarker = rightMarker?.normalized()

    val dragState = rememberDraggableState { delta ->
        if (!hasRange) return@rememberDraggableState
        val deltaValue = (delta / 1000f) * range
        val newValue = (value + deltaValue).coerceIn(min, max)
        onValueChange(newValue)
    }

    val dragModifier = if (hasRange) {
        Modifier.draggable(orientation = Orientation.Horizontal, state = dragState)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(dragModifier)
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
