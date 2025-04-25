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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AmountSlider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.FiatInputField
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BisqAmountSelector(
    quoteCurrencyCode: String,
    formattedMinAmount: String,
    formattedMaxAmount: String,
    sliderPosition: MutableStateFlow<Float>,
    maxSliderValue: StateFlow<Float?> = MutableStateFlow(null),
    leftMarkerSliderValue: StateFlow<Float?> = MutableStateFlow(null),
    rightMarkerSliderValue: StateFlow<Float?> = MutableStateFlow(null),
    formattedFiatAmount: StateFlow<String>,
    formattedBtcAmount: StateFlow<String>,
    onSliderValueChange: (sliderValue: Float) -> Unit,
    onTextValueChange: (String) -> Unit,
    validateTextField: ((String) -> String?)? = null,
) {
    val formattedFiatAmountValue = formattedFiatAmount.collectAsState().value
    val splits = formattedFiatAmountValue.split(".")
    val formattedFiatAmountValueInt = if (splits.isEmpty())
        ""
    else
        splits.first()
    val formattedBtcAmountValue = formattedBtcAmount.collectAsState().value

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        FiatInputField(
            text = formattedFiatAmountValueInt,
            onValueChanged = { onTextValueChange.invoke(it) },
            currency = quoteCurrencyCode,
            validation = {
                if (validateTextField != null) {
                    return@FiatInputField validateTextField(it)
                }
                return@FiatInputField null
            }
        )

        BtcSatsText(formattedBtcAmountValue)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            BisqGap.V3()

            AmountSlider(
                value = sliderPosition,
                maxValue = maxSliderValue,
                leftMarkerValue = leftMarkerSliderValue,
                rightMarkerValue = rightMarkerSliderValue,
                onValueChange = { onSliderValueChange(it) }
            )

            BisqGap.V1()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {
                BisqText.smallRegularGrey("min".i18n() + " $formattedMinAmount")
                BisqText.smallRegularGrey("max".i18n() + " $formattedMaxAmount")
            }
        }
    }
}
