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
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText

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
    val quoteSideMinRangeAmountWithoutDecimal = quoteSideMinRangeAmount.split(".").first()
    val baseSideMinRangeAmount = formattedBaseSideMinRangeAmount.collectAsState().value

    val quoteSideMaxRangeAmount = formattedQuoteSideMaxRangeAmount.collectAsState().value
    val quoteSideMaxRangeAmountWithoutDecimal = quoteSideMaxRangeAmount.split(".").first()
    val baseSideMaxRangeAmount = formattedBaseSideMaxRangeAmount.collectAsState().value

    val smallFont = maxOf(quoteSideMaxRangeAmount.length, quoteSideMinRangeAmount.length) > 6

    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.Start) {
                BisqText.smallRegularGrey("Min")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (smallFont) {
                        BisqText.h3Regular(quoteSideMinRangeAmountWithoutDecimal)
                        BisqText.baseLight(quoteCurrencyCode)
                    } else {
                        BisqText.h2Regular(quoteSideMinRangeAmountWithoutDecimal)
                        BisqText.h6Light(quoteCurrencyCode)
                    }
                }
                BtcSatsText(baseSideMinRangeAmount)
            }
            Column(horizontalAlignment = Alignment.End) {
                BisqText.smallRegularGrey("Max")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (smallFont) {
                        BisqText.h3Regular(quoteSideMaxRangeAmountWithoutDecimal)
                        BisqText.baseLight(quoteCurrencyCode)
                    } else {
                        BisqText.h2Regular(quoteSideMaxRangeAmountWithoutDecimal)
                        BisqText.h6Light(quoteCurrencyCode)
                    }
                }
                BtcSatsText(baseSideMaxRangeAmount)
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
                BisqText.smallLightGrey(formattedMinAmount)
                BisqText.smallLightGrey(formattedMaxAmount)
            }
        }
    }
}