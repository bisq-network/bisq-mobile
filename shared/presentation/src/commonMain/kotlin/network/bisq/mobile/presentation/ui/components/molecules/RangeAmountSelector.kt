package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    minRangeSliderValue: MutableStateFlow<Float>,
    onMinRangeSliderValueChange: (Float) -> Unit,
    maxRangeSliderValue: MutableStateFlow<Float>,
    onMaxRangeSliderValueChange: (Float) -> Unit,
    maxSliderValue: StateFlow<Float?> = MutableStateFlow(null),
    leftMarkerSliderValue: StateFlow<Float?> = MutableStateFlow(null),
    rightMarkerSliderValue: StateFlow<Float?> = MutableStateFlow(null),
    formattedQuoteSideMinRangeAmount: StateFlow<String>,
    formattedBaseSideMinRangeAmount: StateFlow<String>,
    formattedQuoteSideMaxRangeAmount: StateFlow<String>,
    formattedBaseSideMaxRangeAmount: StateFlow<String>,
    onMinAmountTextValueChange: (String) -> Unit, // todo not applied yet
    onMaxAmountTextValueChange: (String) -> Unit, // todo not applied yet
    validateRangeMinTextField: ((String) -> String?)? = null,
    validateRangeMaxTextField: ((String) -> String?)? = null,
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
                    }
                )
                BtcSatsText(baseSideMinRangeAmount)
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
                    }
                )
                BtcSatsText(baseSideMaxRangeAmount)
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