package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.icons.InfoIcon
import network.bisq.mobile.presentation.ui.helpers.numberFormatter
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqAmountSelector(
    minAmount: Float,
    maxAmount: Float,
    exchangeRate: Double,
    currency: String,
    onValueChange: (Float) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf((minAmount + maxAmount) * 0.5f) }
    val roundedNumber = (sliderPosition * 100).roundToInt() / 100.0
    val price = if (roundedNumber.toString().split(".").getOrNull(1)?.length == 1)
        "${roundedNumber}0" // to make 3.1 to 3.10
    else
        roundedNumber.toString() // if it's 3.14, keep the same

    val satsValue = numberFormatter.satsFormat(price.toDouble() / exchangeRate)

    val highLightedSatsZeros = satsValue.takeWhile { it == '0' || it == '.' }
    val sats = satsValue.dropWhile { it == '0' || it == '.' }
    var showPopup by remember { mutableStateOf(false) }
    val satoshi = sats.reversed().chunked(3).joinToString(" ").reversed()
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(sliderPosition) {
        onValueChange(sliderPosition)
    }

    Column(
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TODO: Create a control out of this
                DynamicImage(
                    "drawable/bitcoin.png",
                    modifier = Modifier.size(16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BisqText.h5Regular(
                        text = highLightedSatsZeros,
                        color = BisqTheme.colors.grey2
                    )
                    BisqText.h5Regular(
                        text = "$satoshi sats",
                    )
                }
                BisqGap.H1()
                // TODO: Needs work
                TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        Box(
                            modifier = Modifier.background(BisqTheme.colors.dark5),
                            contentAlignment = Alignment.Center
                        ){
                            BisqText.h5Regular(
                                text = "Hi welcome"
                            )
                        }

                    },
                    // modifier = Modifier.offset((-20).dp, (-20).dp),
                    state = tooltipState
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            tooltipState.show()
                        }
                    }) {
                        // TODO: Make SVG Icons work!
                        InfoIcon()
                    }
                }
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