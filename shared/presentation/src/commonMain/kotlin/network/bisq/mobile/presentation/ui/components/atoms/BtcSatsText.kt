package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

enum class BtcSatsStyle {
    Default, // Regular text
    TextField  // Full width, text field style
}

@Composable
fun BtcSatsText(
    formattedBtcAmountValue: String, // Expect this to be in btc format (Eg: 0.001112222)
    label: String? = null,
    fontSize: FontSize = FontSize.BASE,
    style: BtcSatsStyle = BtcSatsStyle.Default,
    noCode: Boolean = false
) {
    if (formattedBtcAmountValue.isEmpty())
        return;

    val formattedValue = formatSatsToDisplay(formattedBtcAmountValue, noCode)
    val finalFontSize = fontSize

    if (style == BtcSatsStyle.Default) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BtcLogo()
            Text(
                text = formattedValue,
                fontSize = finalFontSize.size,
                fontFamily = BisqText.fontFamilyRegular(),
                lineHeight = TextUnit(finalFontSize.size.times(1.15).value, TextUnitType.Sp),
                color = BisqTheme.colors.white,
            )
        }
    } else if (style == BtcSatsStyle.TextField) {
        val grey2Color = BisqTheme.colors.mid_grey20

        if (label?.isNotEmpty() == true) {
            BisqText.baseLight(
                text = label,
                color = grey2Color,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
            )
            BisqGap.VQuarter()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(6.dp))
                .background(BisqTheme.colors.secondaryDisabled)
                .drawBehind {
                    drawLine(
                        color = grey2Color,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 4.dp.toPx()
                    )
                }
                .padding(BisqUIConstants.ScreenPadding),
        ) {
            // BtcLogo()
            Text(
                text = formattedValue,
                fontSize = finalFontSize.size,
                fontFamily = BisqText.fontFamilyRegular(),
                lineHeight = TextUnit(finalFontSize.size.times(1.15).value, TextUnitType.Sp),
                color = BisqTheme.colors.white,
            )
        }
    }
}

@Composable
private fun formatSatsToDisplay(formattedBtcAmountValue: String, noCode: Boolean): AnnotatedString {

    return buildAnnotatedString {
        val parts = formattedBtcAmountValue.split(".")
        val integerPart = parts[0]
        val fractionalPart = parts[1] ?: ""

        val formattedFractional = fractionalPart.reversed().chunked(3).joinToString(" ").reversed()
        val leadingZeros = formattedFractional.takeWhile { it == '0' || it == ' ' }
        val significantDigits = formattedFractional.dropWhile { it == '0' || it == ' ' }

        val prefixColor = if (integerPart.toInt() > 0) BisqTheme.colors.white else BisqTheme.colors.mid_grey20

        withStyle(style = SpanStyle(color = prefixColor)) {
            append(integerPart)
            append(".")
        }

        withStyle(style = SpanStyle(color = prefixColor)) {
            append(leadingZeros)
        }

        withStyle(style = SpanStyle(color = BisqTheme.colors.white)) {
            append(significantDigits)
            if (noCode == false)
                append(" BTC")
        }
    }
}