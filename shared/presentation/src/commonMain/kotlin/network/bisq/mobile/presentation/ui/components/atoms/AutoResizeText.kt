package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

/**
 * Tries to render the text at given fontSize, but will automatically decrease
 * font size till the text does not overflow anymore
 */
@Composable
fun AutoResizeText(
    text: String,
    color: Color = BisqTheme.colors.white,
    textStyle: TextStyle = BisqTheme.typography.baseRegular,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = BisqText.getDefaultLineHeight(textStyle.fontSize),
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    minimumFontSize: TextUnit = 10.sp,
    modifier: Modifier = Modifier,
) {
    var fontSize by remember { mutableStateOf(textStyle.fontSize) }
    var readyToDraw by remember(text, fontSize, maxLines, overflow) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        style = textStyle,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && fontSize > minimumFontSize) {
                val next = fontSize * 0.9f
                fontSize = if (next < minimumFontSize) minimumFontSize else next
            } else {
                readyToDraw = true
            }
        },
    )
}