package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 100.dp)
        .padding(bottom = 20.dp)
        .height(2.dp)
) {
    
    val grey2Color = BisqTheme.colors.grey2

    LinearProgressIndicator(
        progress = {progress},
        modifier = modifier,
        trackColor = BisqTheme.colors.grey2,
        color = BisqTheme.colors.primary,
        gapSize = 0.dp,
        drawStopIndicator = {
            drawStopIndicator(
                drawScope = this,
                stopSize = 0.dp,
                color = grey2Color,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )
        }
    )
}