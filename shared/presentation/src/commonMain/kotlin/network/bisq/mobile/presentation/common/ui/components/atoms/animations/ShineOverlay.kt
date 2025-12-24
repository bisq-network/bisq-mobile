package network.bisq.mobile.presentation.common.ui.components.atoms.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.random.Random

const val INITIAL_SHINE = -1.0f
const val TARGET_SHINE = 3f
const val ANIMATION_INTERVAL = 8000
const val ANIMATION_MAX_INTERVAL = 12001
const val GRADIENT_OFFSET_FACTOR = 300f

const val SHINE_SWEEP_DURATION_MS = 4000

fun nextDuration(): Int = Random.nextInt(ANIMATION_INTERVAL, ANIMATION_MAX_INTERVAL)

@Composable
fun ShineOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Layer composable with shine overlay
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        content()
        ShineCanvasOverlay(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(CircleShape),
        )
    }
}

@Composable
private fun ShineCanvasOverlay(modifier: Modifier = Modifier) {
    val anim = remember { Animatable(INITIAL_SHINE) }
    var isAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(nextDuration().toLong())
            anim.snapTo(INITIAL_SHINE)
            isAnimating = true
            anim.animateTo(
                targetValue = TARGET_SHINE,
                animationSpec = tween(durationMillis = SHINE_SWEEP_DURATION_MS, easing = LinearEasing),
            )
            isAnimating = false
            anim.snapTo(INITIAL_SHINE)
        }
    }
    Canvas(modifier = modifier) {
        if (!isAnimating) return@Canvas
        val t = anim.value
        val start = Offset(t * GRADIENT_OFFSET_FACTOR, t * GRADIENT_OFFSET_FACTOR)
        val end = Offset((t + 1) * GRADIENT_OFFSET_FACTOR, (t + 1) * GRADIENT_OFFSET_FACTOR)
        val brush =
            Brush.linearGradient(
                colorStops =
                    arrayOf(
                        0.0f to Color.Transparent,
                        0.35f to Color.White.copy(alpha = 0.3f),
                        0.65f to Color.White.copy(alpha = 0.3f),
                        1.0f to Color.Transparent,
                    ),
                start = start,
                end = end,
            )
        drawRect(brush = brush)
    }
}
