package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    minimumFontSize: TextUnit = 10.sp,
    modifier: Modifier = Modifier,
) {
    var readyToDraw by remember(text, textStyle.fontSize, maxLines, overflow) {
        mutableStateOf(false)
    }
    var determinedFontSize by remember(text, textStyle.fontSize) {
        mutableStateOf(textStyle.fontSize)
    }
    val determinedLineHeight by remember {
        derivedStateOf {
            if (lineHeight == TextUnit.Unspecified) {
                determinedFontSize * 1.15f
            } else {
                lineHeight
            }
        }
    }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        style = textStyle.copy(fontSize = determinedFontSize),
        textAlign = textAlign,
        lineHeight = determinedLineHeight,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && determinedFontSize > minimumFontSize) {
                val next = determinedFontSize * 0.9f
                determinedFontSize = if (next < minimumFontSize) minimumFontSize else next
            } else {
                readyToDraw = true
            }
        },
    )
}