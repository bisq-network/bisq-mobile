package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqSlider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.SvgImage
import network.bisq.mobile.presentation.ui.components.atoms.SvgImageNames
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqAmountSelector(
    minAmount: Float,
    maxAmount: Float,
    exchangeRate: Double,
    currency: String
) {
    var sliderPosition by remember { mutableFloatStateOf((minAmount + maxAmount) * 0.5f) }
    val roundedNumber = (sliderPosition * 100).roundToInt() / 100.0
    val price = if (roundedNumber.toString().split(".").getOrNull(1)?.length == 1)
        "${roundedNumber}0" // to make 3.1 to 3.10
    else
        roundedNumber.toString() // if it's 3.14, keep the same

    val satsValue = (price.toDouble() / exchangeRate).toString()

    Column(
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BisqText.h1Regular(
                    text = price,
                )
                BisqText.h5Regular(
                    text = currency
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // TODO: Do the btc-sats display control, as suggested by cbeams
                BisqText.h5Regular(
                    text = "$satsValue btc",
                    color = BisqTheme.colors.grey2
                )
                SvgImage(
                    image = SvgImageNames.INFO,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(BisqTheme.colors.grey2)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            BisqSlider(
                minAmount,
                maxAmount,
                sliderPosition,
                onValueChange = { sliderPosition = it }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {

                val minString = minAmount // Do precision rounding to 2 decimals
                val maxString = maxAmount // Do precision rounding to 2 decimals
                BisqText.smallRegular(
                    text = "Min $minString $currency",
                    color = BisqTheme.colors.grey2
                )
                BisqText.smallRegular(
                    text = "Max $maxString $currency",
                    color = BisqTheme.colors.grey2
                )
            }
        }
    }
}