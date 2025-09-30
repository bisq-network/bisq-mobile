package network.bisq.mobile.presentation.ui.components.atoms.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlin.random.Random

const val INITIAL_SHINE = -1.0f
const val TARGET_SHINE = 3f
const val ANIMATION_INTERVAL = 8000
const val ANIMATION_MAX_INTERVAL = 12001
const val GRADIENT_OFFSET_FACTOR = 300f

fun nextDuration(): Int = Random.nextInt(ANIMATION_INTERVAL, ANIMATION_MAX_INTERVAL)

@Composable
fun ShineOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    val randomDuration = remember { mutableStateOf(nextDuration()) }

    val infiniteTransition = rememberInfiniteTransition()
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = INITIAL_SHINE,
        targetValue = TARGET_SHINE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = randomDuration.value, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(gradientOffset) {
        if (gradientOffset == TARGET_SHINE) {
            randomDuration.value = nextDuration()
        }
    }

    // Gradient brush that moves across the composable
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.3f), // Shine effect
            Color.Transparent
        ),
        start = Offset(gradientOffset * GRADIENT_OFFSET_FACTOR, gradientOffset * GRADIENT_OFFSET_FACTOR),
        end = Offset((gradientOffset + 1) * GRADIENT_OFFSET_FACTOR, (gradientOffset + 1) * GRADIENT_OFFSET_FACTOR)
    )

    // Layer composable with shine overlay
    Box(
        modifier = modifier,
            //.clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UserIcon composable
        content()

        // Canvas for the moving shine gradient
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)) {
            drawRect(brush = gradientBrush)
        }
    }
}