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
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.SliderWithMarker
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.FiatInputField
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
    leftMarkerQuoteSideValue: StateFlow<Float>,
    rightMarkerQuoteSideValue: StateFlow<Float>,
    formattedFiatAmount: StateFlow<String>,
    formattedBtcAmount: StateFlow<String>,
    onSliderValueChange: (sliderValue: Float) -> Unit,
    onTextValueChange: (String) -> Unit
) {
    val formattedFiatAmountValue = formattedFiatAmount.collectAsState().value
    val formattedBtcAmountValue = formattedBtcAmount.collectAsState().value

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        FiatInputField(
            text = formattedFiatAmountValue,
            onValueChanged = { onTextValueChange.invoke(it) },
            enabled = false,
            currency = fiatCurrencyCode
        )

        BtcSatsText(formattedBtcAmountValue)

        Column {
            SliderWithMarker(
                initialValue = initialSliderPosition,
                leftMarkerQuoteSideValue = leftMarkerQuoteSideValue,
                rightMarkerQuoteSideValue = rightMarkerQuoteSideValue,
                onValueChange = { onSliderValueChange(it) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {
                BisqText.smallRegularGrey("Min $formattedMinAmount")
                BisqText.smallRegularGrey("Max $formattedMaxAmount")
            }
        }
    }
}
