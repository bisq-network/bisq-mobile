package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BtcSatsText(
    formattedBtcAmountValue: String,
    fontSize: FontSize = FontSize.BASE,
) {
    val formattedValue = formatSatsToDisplay(formattedBtcAmountValue)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BtcLogo()
        Text(
            text = formattedValue,
            fontSize = fontSize.size,
            fontFamily = BisqText.fontFamilyRegular(),
            lineHeight = TextUnit(fontSize.size.times(1.15).value, TextUnitType.Sp),
            color = BisqTheme.colors.white,
        )
    }
}

@Composable
private fun formatSatsToDisplay(formattedBtcAmountValue: String): AnnotatedString {

    return buildAnnotatedString {
        val parts = formattedBtcAmountValue.split(".")
        val integerPart = parts[0]
        val fractionalPart = parts[1] ?: ""

        val formattedFractional = fractionalPart.reversed().chunked(3).joinToString(" ").reversed()
        val leadingZeros = formattedFractional.takeWhile { it == '0' || it == ' ' }
        val significantDigits = formattedFractional.dropWhile { it == '0' || it == ' ' }

        val prefixColor = if(integerPart.toInt() > 0) BisqTheme.colors.white else BisqTheme.colors.grey2

        withStyle(style = SpanStyle(color = prefixColor)) {
            append(integerPart)
            append(".")
        }

        withStyle(style = SpanStyle(color = prefixColor)) {
            append(leadingZeros)
        }

        withStyle(style = SpanStyle(color = BisqTheme.colors.white)) {
            append(significantDigits)
            append(" BTC")
        }
    }
}