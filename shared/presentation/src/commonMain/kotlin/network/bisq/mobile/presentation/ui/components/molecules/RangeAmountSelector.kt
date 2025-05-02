package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.getDecimalSeparator
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.RangeAmountSlider
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.FiatInputField

// TODO: This has more work to do
@Composable
fun RangeAmountSelector(
    formattedMinAmount: String,
    formattedMaxAmount: String,
    quoteCurrencyCode: String,
    minRangeSliderValue: Float,
    onMinRangeSliderValueChange: (Float) -> Unit,
    maxRangeSliderValue: Float,
    onMaxRangeSliderValueChange: (Float) -> Unit,
    maxSliderValue: Float? = null,
    leftMarkerSliderValue: Float? = null,
    rightMarkerSliderValue: Float? = null,
    formattedQuoteSideMinRangeAmount: String,
    formattedBaseSideMinRangeAmount: String,
    formattedQuoteSideMaxRangeAmount: String,
    formattedBaseSideMaxRangeAmount: String,
    onMinAmountTextValueChange: (String) -> Unit,
    onMaxAmountTextValueChange: (String) -> Unit,
    validateRangeMinTextField: ((String) -> String?)? = null,
    validateRangeMaxTextField: ((String) -> String?)? = null,
) {
    val decimalSeparator = getDecimalSeparator()
    val quoteSideMinRangeAmountWithoutDecimal = formattedQuoteSideMinRangeAmount.substringBefore(decimalSeparator)
    val quoteSideMaxRangeAmountWithoutDecimal = formattedQuoteSideMaxRangeAmount.substringBefore(decimalSeparator)

    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1.0F)
            ) {
                BisqText.smallRegularGrey("min".i18n())
                FiatInputField(
                    text = quoteSideMinRangeAmountWithoutDecimal,
                    onValueChanged = { onMinAmountTextValueChange.invoke(it) },
                    currency = quoteCurrencyCode,
                    textAlign = TextAlign.Start,
                    validation = {
                        if (validateRangeMinTextField != null) {
                            return@FiatInputField validateRangeMinTextField(it)
                        }
                        return@FiatInputField null
                    },
                    smallFont = true
                )
                BtcSatsText(formattedBaseSideMinRangeAmount)
            }
            BisqGap.H1()
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1.0F)
            ) {
                BisqText.smallRegularGrey("max".i18n())
                FiatInputField(
                    text = quoteSideMaxRangeAmountWithoutDecimal,
                    onValueChanged = { onMaxAmountTextValueChange.invoke(it) },
                    currency = quoteCurrencyCode,
                    textAlign = TextAlign.Start,
                    validation = {
                        if (validateRangeMaxTextField != null) {
                            return@FiatInputField validateRangeMaxTextField(it)
                        }
                        return@FiatInputField null
                    },
                    smallFont = true
                )
                BtcSatsText(formattedBaseSideMaxRangeAmount)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            BisqGap.V3()

            RangeAmountSlider(
                minRangeValue = minRangeSliderValue,
                onMinRangeValueChange = onMinRangeSliderValueChange,
                maxRangeValue = maxRangeSliderValue,
                onMaxRangeValueChange = onMaxRangeSliderValueChange,
                maxValue = maxSliderValue,
                leftMarkerValue = leftMarkerSliderValue,
                rightMarkerValue = rightMarkerSliderValue,
            )

            BisqGap.V1()

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