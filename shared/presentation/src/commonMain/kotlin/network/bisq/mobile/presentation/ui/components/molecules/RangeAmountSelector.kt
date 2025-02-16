package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@OptIn(ExperimentalMaterial3Api::class)
// TODO: This has more work to do
@Composable
fun RangeAmountSelector(
    formattedMinAmount: String,
    formattedMaxAmount: String,
    quoteCurrencyCode: String,
    initialSliderPosition: ClosedFloatingPointRange<Float>,
    formattedQuoteSideMinRangeAmount: StateFlow<String>,
    formattedBaseSideMinRangeAmount: StateFlow<String>,
    formattedQuoteSideMaxRangeAmount: StateFlow<String>,
    formattedBaseSideMaxRangeAmount: StateFlow<String>,
    onSliderValueChange: (sliderValue: ClosedFloatingPointRange<Float>) -> Unit,
    onMinAmountTextValueChange: (String) -> Unit, // todo not applied yet
    onMaxAmountTextValueChange: (String) -> Unit // todo not applied yet
) {
    val quoteSideMinRangeAmount = formattedQuoteSideMinRangeAmount.collectAsState().value
    val baseSideMinRangeAmount = formattedBaseSideMinRangeAmount.collectAsState().value
    val quoteSideMaxRangeAmount = formattedQuoteSideMaxRangeAmount.collectAsState().value
    val baseSideMaxRangeAmount = formattedBaseSideMaxRangeAmount.collectAsState().value

    val baseSideMinRangeAmountLeft = baseSideMinRangeAmount
        .takeWhile { it == '0' || it == '.' }
    val baseSideMinRangeAmountRight = baseSideMinRangeAmount
        .dropWhile { it == '0' || it == '.' }
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    val baseSideMaxRangeAmountLeft = baseSideMaxRangeAmount
        .takeWhile { it == '0' || it == '.' }
    val baseSideMaxRangeAmountRight = baseSideMaxRangeAmount
        .dropWhile { it == '0' || it == '.' }
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.Start) {
                BisqText.smallRegularGrey(text = "Min")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BisqText.h2Regular(text = quoteSideMinRangeAmount)
                    BisqText.h6Light(text = quoteCurrencyCode)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    /* DynamicImage(
                         "drawable/bitcoin.png",
                         modifier = Modifier.size(16.dp)
                     )*/
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BisqText.largeLightGrey(text = baseSideMinRangeAmountLeft)
                        BisqText.largeLight(
                            text = "$baseSideMinRangeAmountRight BTC",
                        )
                    }
                    /* SvgImage(
                         image = SvgImageNames.INFO,
                         modifier = Modifier.size(16.dp),
                         colorFilter = ColorFilter.tint(BisqTheme.colors.grey2)
                     )*/
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                BisqText.smallRegularGrey(text = "Max")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BisqText.h2Regular(
                        text = quoteSideMaxRangeAmount
                    )
                    BisqText.h6Light(
                        text = quoteCurrencyCode
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    /* DynamicImage(
                         "drawable/bitcoin.png",
                         modifier = Modifier.size(16.dp)
                     )*/
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BisqText.largeLightGrey(text = baseSideMaxRangeAmountLeft)
                        BisqText.largeLight(
                            text = "$baseSideMaxRangeAmountRight BTC",
                        )
                    }
                    /*  SvgImage(
                          image = SvgImageNames.INFO,
                          modifier = Modifier.size(16.dp),
                          colorFilter = ColorFilter.tint(BisqTheme.colors.grey2)
                      )*/
                }

            }

        }
        Column {
            BisqRangeSlider(
                initialSliderPosition,
                { onSliderValueChange.invoke(it) }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {

                BisqText.smallLightGrey(text = formattedMinAmount)
                BisqText.smallLightGrey(text = formattedMaxAmount)
            }
        }
    }
}