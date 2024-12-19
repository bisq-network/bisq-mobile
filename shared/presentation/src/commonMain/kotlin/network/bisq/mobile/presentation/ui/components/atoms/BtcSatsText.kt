package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.icons.BtcLogo
import network.bisq.mobile.presentation.ui.helpers.numberFormatter
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BtcSatsText(sats: Long) {

    val btcValue = sats.toDouble() / 100_000_000
    val formattedValue = formatSatsToDisplay(btcValue, sats)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BtcLogo()
        Text(
            text = formattedValue,
            fontSize = 20.sp,
            color = BisqTheme.colors.light1,
        )
    }
}

@Composable
private fun formatSatsToDisplay(btcValue: Double, sats: Long): AnnotatedString {
    val btcString = numberFormatter.satsFormat(btcValue)

    return buildAnnotatedString {
        val parts = btcString.split(".")
        val integerPart = parts[0]
        val fractionalPart = parts[1]

        val formattedFractional = fractionalPart.chunked(3).joinToString(" ")
        val leadingZeros = formattedFractional.takeWhile { it == '0' || it == ' ' }
        val significantDigits = formattedFractional.dropWhile { it == '0' || it == ' ' }

        val prefixColor = if(integerPart.toInt() > 0) BisqTheme.colors.light1 else BisqTheme.colors.grey2

        withStyle(style = SpanStyle(color = prefixColor)) {
            append(integerPart)
            append(".")
        }

        withStyle(style = SpanStyle(color = prefixColor)) {
            append(leadingZeros)
        }

        withStyle(style = SpanStyle(color = BisqTheme.colors.light1)) {
            append(significantDigits)
            append(" sats")
        }
    }
}