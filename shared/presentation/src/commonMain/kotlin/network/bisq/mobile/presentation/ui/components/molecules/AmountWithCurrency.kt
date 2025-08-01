package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap

// Highly tied to how formattedPrice is formatted.
@Composable
fun AmountWithCurrency(
    formattedPrice: String,
) {
    val isRangeAmount = formattedPrice.contains("-")

    if(isRangeAmount) {
        val priceRange = formattedPrice.split("-")
        if (priceRange.size != 2) {
            // Fallback to single amount display if parsing fails
            SingleAmountWithCurrency(formattedPrice)
            return
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SingleAmountWithCurrency(priceRange[0].trim())
            BisqGap.HHalf()
            BisqText.largeRegular(text = "-")
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
        verticalAlignment = Alignment.Bottom,
    ) {
        BisqText.largeRegular(text = value)

        if(priceFragments.size == 2) {
            BisqGap.HHalf()
            BisqText.baseRegularGrey(priceFragments[1])
        }
    }
}