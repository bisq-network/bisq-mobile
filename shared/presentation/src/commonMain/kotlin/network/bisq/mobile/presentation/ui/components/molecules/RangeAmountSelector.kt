package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.SvgImage
import network.bisq.mobile.presentation.ui.components.atoms.SvgImageNames
import network.bisq.mobile.presentation.ui.helpers.numberFormatter
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
// TODO: This has more work to do
@Composable
fun RangeAmountSelector(
    minAmount: Double,
    maxAmount: Double,
) {
    var sliderPosition by remember { mutableStateOf(minAmount..maxAmount) }
    var tradeValue = 873f..1200f
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.Start) {
                BisqText.xsmallRegular(
                    text = "Min",
                    color = BisqTheme.colors.grey1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BisqText.h5Regular(
                        text = sliderPosition.start.toString()
                    )
                    BisqText.xsmallRegular(
                        text = "USD"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DynamicImage(
                        "drawable/bitcoin.png",
                        modifier = Modifier.size(16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BisqText.smallRegular(
                            text = "0.00",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.smallRegular(
                            text = "273 116 sats",
                        )
                    }
                    SvgImage(
                        image = SvgImageNames.INFO,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(BisqTheme.colors.grey2)
                    )
                }

            }
            Column(horizontalAlignment = Alignment.End) {
                BisqText.xsmallRegular(
                    text = "Max",
                    color = BisqTheme.colors.grey1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BisqText.h5Regular(
                        text = sliderPosition.endInclusive.toString()
                    )
                    BisqText.xsmallRegular(
                        text = "USD"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DynamicImage(
                        "drawable/bitcoin.png",
                        modifier = Modifier.size(16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                        BisqText.smallRegular(
                            text = "0.00",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.smallRegular(
                            text = "273 116 sats",
                        )
                    }
                    SvgImage(
                        image = SvgImageNames.INFO,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(BisqTheme.colors.grey2)
                    )
                }

            }

        }
        Column {
            BisqRangeSlider(
                sliderPosition.start.toFloat()..sliderPosition.endInclusive.toFloat(),
                onValueChange = {
                    sliderPosition = it.start.toDouble()..it.endInclusive.toDouble()
                },
                minAmount.toFloat(),
                maxAmount.toFloat(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {

                val minString = minAmount // Do precision rounding to 2 decimals
                val maxString = maxAmount // Do precision rounding to 2 decimals
                BisqText.smallRegular(
                    text = "Min $minString USD",
                    color = BisqTheme.colors.grey2
                )
                BisqText.smallRegular(
                    text = "Max $maxString USD",
                    color = BisqTheme.colors.grey2
                )
            }
        }
    }
}