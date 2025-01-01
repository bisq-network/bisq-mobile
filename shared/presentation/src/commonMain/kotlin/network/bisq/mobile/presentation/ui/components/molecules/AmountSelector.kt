package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.components.atoms.BisqSlider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// ToDiscuss:
// buddha: Ideally this component should deal only with Fiat values (as Double) and have one valueChange() event
// so `initialSliderPosition` will become `defaultValue`,
// which will be some value between `formattedMinAmount` and `formattedMaxAmount`
// onSliderValueChange() / onTextValueChange() will become onValueChange(value: Double) -> Unit
@Composable
fun BisqAmountSelector(
    fiatCurrencyCode: String,
    formattedMinAmount: String,
    formattedMaxAmount: String,
    initialSliderPosition: Float,
    formattedFiatAmount: StateFlow<String>,
    formattedBtcAmount: StateFlow<String>,
    onSliderValueChange: (sliderValue: Float) -> Unit,
    onTextValueChange: (String) -> Unit
) {
    val formattedFiatAmountValue = formattedFiatAmount.collectAsState().value
    val formattedBtcAmountValue = formattedBtcAmount.collectAsState().value

    val btcAmountValueHighLightedZeros = formattedBtcAmountValue
        .takeWhile { it == '0' || it == '.' }
    val btcAmountValue = formattedBtcAmountValue
        .dropWhile { it == '0' || it == '.' }
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    /* var fiatValue by remember { mutableDoubleStateOf((minAmount + maxAmount) * 0.5) }
     val sats = (100_000_000L * (fiatValue.toDouble()) / exchangeRate).toLong()

     LaunchedEffect(fiatValue) {
         onValueChange?.invoke(fiatValue)
     }*/

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        FiatInputField(
            text = formattedFiatAmountValue,
            onValueChanged = { onTextValueChange.invoke(it) },
            currency = fiatCurrencyCode
        )

        /*if (fiatValue < minAmount || fiatValue > maxAmount) {
            BisqText.baseRegular("Amount out of range", color = BisqTheme.colors.danger)
        }*/

        BtcSatsText(formattedBtcAmountValue)

        Column {
            BisqSlider(
                initialSliderPosition,
                { onSliderValueChange(it) }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {
                BisqText.smallRegular(
                    text = "Min $formattedMinAmount",
                    color = BisqTheme.colors.grey2
                )
                BisqText.smallRegular(
                    text = "Max $formattedMaxAmount",
                    color = BisqTheme.colors.grey2
                )
            }
        }
    }
}
