package network.bisq.mobile.presentation.common.ui.components.atoms

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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqTypography

/**
 * Tries to render the text at given fontSize, but will automatically decrease
 * font size till the text does not overflow anymore
 */
@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BisqTheme.colors.white,
    textStyle: TextStyle = BisqTheme.typography.baseRegular,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    minimumFontSize: TextUnit = 10.sp,
    softWrap: Boolean = true,
    hideWhenOverflowingAtMinimum: Boolean = false,
) {
    AutoResizeText(
        text = AnnotatedString(text),
        modifier = modifier,
        color = color,
        textStyle = textStyle,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        minimumFontSize = minimumFontSize,
        softWrap = softWrap,
        hideWhenOverflowingAtMinimum = hideWhenOverflowingAtMinimum,
    )
}

/**
 * @param hideWhenOverflowingAtMinimum when the text still overflows at [minimumFontSize], draw
 * nothing instead of clipping it. For optional labels where a cut-off value is worse than none.
 * The text stays in the semantics tree, and a wider layout draws it again.
 */
@Composable
fun AutoResizeText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = BisqTheme.colors.white,
    textStyle: TextStyle = BisqTheme.typography.baseRegular,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    minimumFontSize: TextUnit = 10.sp,
    softWrap: Boolean = true,
    hideWhenOverflowingAtMinimum: Boolean = false,
) {
    val inPreview = LocalInspectionMode.current
    var readyToDraw by remember(text, textStyle.fontSize, maxLines, overflow) {
        mutableStateOf(false)
    }
    var hidden by remember(text, textStyle.fontSize, maxLines, overflow) {
        mutableStateOf(false)
    }
    var determinedFontSize by remember(text, textStyle.fontSize) {
        mutableStateOf(textStyle.fontSize)
    }
    val determinedLineHeight by remember {
        derivedStateOf {
            if (lineHeight == TextUnit.Unspecified) {
                determinedFontSize * BisqTypography.LINE_HEIGHT_MULTIPLIER
            } else {
                lineHeight
            }
        }
    }

    Text(
        text = text,
        modifier =
            modifier.drawWithContent {
                // During previews, the `onTextLayout` recomposition loop may not complete,
                // which would leave `readyToDraw` as false and result in an empty render.
                // Bypassing the check in preview mode ensures the component is always visible
                // for development, without affecting runtime behavior.
                if ((readyToDraw || inPreview) && !hidden) drawContent()
            },
        color = color,
        style = textStyle.copy(fontSize = determinedFontSize),
        textAlign = textAlign,
        lineHeight = determinedLineHeight,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { textLayoutResult ->
            when (
                val step =
                    autoResizeStep(
                        textLayoutResult.hasVisualOverflow,
                        determinedFontSize,
                        minimumFontSize,
                        hideWhenOverflowingAtMinimum,
                    )
            ) {
                is AutoResizeStep.Shrink -> determinedFontSize = step.fontSize
                AutoResizeStep.Draw -> {
                    hidden = false
                    readyToDraw = true
                }
                AutoResizeStep.Hide -> {
                    hidden = true
                    readyToDraw = true
                }
            }
        },
        softWrap = softWrap,
    )
}

internal sealed interface AutoResizeStep {
    data class Shrink(
        val fontSize: TextUnit,
    ) : AutoResizeStep

    data object Draw : AutoResizeStep

    data object Hide : AutoResizeStep
}

/** One pass of the shrink loop: overflowing text loses 10% per layout until [minimumFontSize]. */
internal fun autoResizeStep(
    hasVisualOverflow: Boolean,
    fontSize: TextUnit,
    minimumFontSize: TextUnit,
    hideWhenOverflowingAtMinimum: Boolean = false,
): AutoResizeStep =
    when {
        !hasVisualOverflow -> AutoResizeStep.Draw
        fontSize > minimumFontSize -> {
            val next = fontSize * 0.9f
            AutoResizeStep.Shrink(if (next < minimumFontSize) minimumFontSize else next)
        }
        hideWhenOverflowingAtMinimum -> AutoResizeStep.Hide
        else -> AutoResizeStep.Draw
    }
