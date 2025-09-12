package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqStepProgressBar(
    stepIndex: Int,
    stepsLength: Int,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(BisqUIConstants.ScreenPadding2),
    gapSize: Dp = BisqUIConstants.ScreenPadding2,
    barHeight: Dp = BisqUIConstants.ScreenPadding2,
) {
    val activeColor = BisqTheme.colors.primary
    val inactiveColor = BisqTheme.colors.mid_grey20

    Row(modifier = modifier) {
        for (index in 1..stepsLength) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .background(if (index <= stepIndex) activeColor else inactiveColor)
            )
            if (index < stepsLength) {
                Spacer(modifier = Modifier.width(gapSize))
            }
        }
    }
}