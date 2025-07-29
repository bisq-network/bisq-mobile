package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

/**
 * Jetpack Compose version of the JavaFX TextList.
 * @param text The input text to split and render as a list.
 * @param style A lambda to render text with a BisqText style (e.g., BisqText.baseRegular).
 * @param gap Horizontal gap between mark and text.
 * @param vSpacing Vertical spacing between list items.
 * @param regex Regex to split the text.
 * @param mark Optional mark string for all items. If null, uses getMark.
 * @param getMark Function to generate mark for each item (used when mark is null).
 */
@Composable
fun TextList(
    text: String,
    style: @Composable (String, Modifier) -> Unit = { t, m -> BisqText.baseRegular(text = t, modifier = m) },
    gap: Dp = BisqUIConstants.ScreenPaddingHalfQuarter,
    vSpacing: Dp = BisqUIConstants.ScreenPaddingHalfQuarter,
    regex: String,
    mark: String? = null,
    getMark: (Int) -> String = { "$it. " },
    modifier: Modifier = Modifier,
) {
    val compiledRegex = remember(regex) { regex.toRegex() }
    val list = text.split(compiledRegex).filter { it.isNotBlank() }
    if (list.isEmpty() || (list.size == 1 && list[0] == text)) {
        // Single item, render as plain text
        style(text, modifier)
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(vSpacing)
    ) {
        var i = 0
        for (item in list) {
            val content = item.trim()
            if (content.isEmpty()) continue
            i++
            val markString = mark ?: getMark(i)
            Row {
                style(markString, Modifier)
                Spacer(Modifier.width(gap))
                style(content, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun UnorderedTextList(
  text: String,
  style: @Composable (String, Modifier) -> Unit = { t, m -> BisqText.baseRegular(text = t, modifier = m) },
  gap: Dp = BisqUIConstants.ScreenPaddingHalfQuarter,
  vSpacing: Dp = BisqUIConstants.ScreenPaddingHalfQuarter,
  regex: String = "- ",
  mark: String = "\u2022", // Unicode bullet
  modifier: Modifier = Modifier,
) {
  TextList(
    text = text,
    style = style,
    gap = gap,
    vSpacing = vSpacing,
    regex = regex,
    mark = mark,
    modifier = modifier
  )
}

/**
 * Renders an ordered (numbered) list. Note: Original numbering is ignored and items are renumbered starting from 1.
 */
@Composable
fun OrderedTextList(
  text: String,
  style: @Composable (String, Modifier) -> Unit = { t, m -> BisqText.baseRegular(text = t, modifier = m) },
  gap: Dp = BisqUIConstants.ScreenPaddingHalfQuarter,
  vSpacing: Dp = BisqUIConstants.ScreenPaddingHalfQuarter,
  regex: String = "\\d+\\.\\s*",
  modifier: Modifier = Modifier,
) {
  TextList(
    text = text,
    style = style,
    gap = gap,
    vSpacing = vSpacing,
    regex = regex,
    mark = null,
    modifier = modifier
  )
}
