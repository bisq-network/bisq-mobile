package network.bisq.mobile.presentation.ui.uicases.open_trades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun OpenTradeCard(
    modifier: Modifier = Modifier,
    padding: Dp = BisqUIConstants.ScreenPadding,
    borderRadius: Dp = BisqUIConstants.ScreenPaddingHalf,
    borderWidth: Dp = 4.dp,
    borderColor: Color = Color.Transparent,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    backgroundColor: Color = BisqTheme.colors.dark_grey40,
    hasNotifications: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    // Pick shape based on notification state
    val shape = if (hasNotifications) {
        RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = borderRadius,
            bottomEnd = borderRadius
        )
    } else {
        RoundedCornerShape(borderRadius)
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (hasNotifications && borderColor != Color.Transparent && borderWidth > 0.dp) {
                    Modifier.drawBehind {
                        val strokeWidthPx = borderWidth.toPx()
                        // vertical line on left edge
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = strokeWidthPx
                        )
                    }
                } else Modifier
            )
            .padding(padding)
            .fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}
