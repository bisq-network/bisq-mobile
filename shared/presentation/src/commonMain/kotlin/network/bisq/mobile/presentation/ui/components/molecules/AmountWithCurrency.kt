package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap

@Composable
fun AmountWithCurrency(
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