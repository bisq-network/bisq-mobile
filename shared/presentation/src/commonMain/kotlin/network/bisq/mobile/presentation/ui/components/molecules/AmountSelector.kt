package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.domain.getDecimalSeparator
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
    sliderPosition: Float,
    maxSliderValue: Float? = null,
    leftMarkerSliderValue: Float? = null,
    rightMarkerSliderValue: Float? = null,
    formattedFiatAmount: String,
    formattedBtcAmount: String,
    onSliderValueChange: (sliderValue: Float) -> Unit,
    onTextValueChange: (String) -> Unit,
    validateTextField: ((String) -> String?)? = null,
) {
    val decimalSeparator = getDecimalSeparator()
    val formattedFiatAmountValueInt = formattedFiatAmount.substringBefore(decimalSeparator)

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

        BtcSatsText(formattedBtcAmount)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            BisqGap.V3()

            AmountSlider(
                value = sliderPosition,
                max = maxSliderValue,
                leftMarker = leftMarkerSliderValue,
                rightMarker = rightMarkerSliderValue,
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
