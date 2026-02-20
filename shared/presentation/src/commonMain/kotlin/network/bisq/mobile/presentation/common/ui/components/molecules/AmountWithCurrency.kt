package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

// Highly tied to how formattedPrice is formatted.
@Composable
fun AmountWithCurrency(
    formattedPrice: String,
) {
    val isRangeAmount = formattedPrice.contains("-")

    if (isRangeAmount) {
        val priceRange = formattedPrice.split("-")
        if (priceRange.size != 2) {
            // Fallback to single amount display if parsing fails
            SingleAmountWithCurrency(formattedPrice)
            return
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SingleAmountWithCurrency(priceRange[0].trim())
            BisqGap.HHalf()
            BisqText.H6Light(text = "-")
            BisqGap.HHalf()
            SingleAmountWithCurrency(priceRange[1].trim())
        }
    } else {
        SingleAmountWithCurrency(formattedPrice)
    }
}

@Composable
private fun SingleAmountWithCurrency(
    formattedPrice: String,
) {
    val priceFragments = formattedPrice.split(" ")
    val value = priceFragments[0]
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.H6Light(text = value)

        if (priceFragments.size == 2) {
            BisqGap.HHalf()
            BisqText.BaseRegularGrey(priceFragments[1])
        }
    }
}

@Preview
@Composable
private fun AmountWithCurrency_SingleAmountPreview() {
    BisqTheme.Preview {
        AmountWithCurrency(formattedPrice = "100.50 USD")
    }
}

@Preview
@Composable
private fun AmountWithCurrency_RangeAmountPreview() {
    BisqTheme.Preview {
        AmountWithCurrency(formattedPrice = "100.50 USD - 200.75 USD")
    }
}

@Preview
@Composable
private fun AmountWithCurrency_NoCurrencyPreview() {
    BisqTheme.Preview {
        AmountWithCurrency(formattedPrice = "100.50")
    }
}

@Preview
@Composable
private fun AmountWithCurrency_InvalidRangeFallbackPreview() {
    BisqTheme.Preview {
        // Invalid range format will fall back to single amount display
        AmountWithCurrency(formattedPrice = "100.50-USD-200.75")
    }
}
