package network.bisq.mobile.presentation.ui.components.atoms.animations

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun AnimatedBadge(
    text: String,
    contentColor: Color = BisqTheme.colors.white,
    badgeColor: Color = BisqTheme.colors.yellow,
    showAnimation: Boolean = false,
    xOffset: Dp = 8.dp,
    yOffset: Dp = (-8).dp,
    content: (@Composable RowScope.() -> Unit)? = null
) {

    val scale: Float

    if (showAnimation) {
        val transition = rememberInfiniteTransition(label = "badgePulse")
        scale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 5600 // 600ms pulse + 5000ms delay
                    1.2f at 300 // halfway scale up
                    1f at 600 // back to normal (end of pulse)
                    1f at 5600 // hold until next repeat
                },
                repeatMode = RepeatMode.Restart
            ), label = "scale"
        ).value
    } else {
        scale = 1f
    }

    Badge(
        containerColor = badgeColor,
        contentColor = contentColor,
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (content == null) {
            BisqText.xsmallMedium(
                text = text,
                textAlign = TextAlign.Center,
                color = contentColor,
            )
        } else {
            content()
        }
    }
}