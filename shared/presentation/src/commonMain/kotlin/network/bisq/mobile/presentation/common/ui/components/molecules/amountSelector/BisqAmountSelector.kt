package network.bisq.mobile.presentation.common.ui.components.molecules.amountSelector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.getDecimalSeparator
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.slider.BisqSlider
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqFiatInputField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A composable that allows users to select an amount using either a text input field or a slider,
 * displaying both fiat and BTC values.
 *
 * This component combines a [BisqFiatInputField] for direct text input, a [BtcSatsText] showing
 * the BTC equivalent, and a [BisqSlider] for visual amount selection with min/max labels.
 *
 * @param modifier Modifier to be applied to the root container
 * @param quoteCurrencyCode The currency code to display as suffix in the input field (e.g., "USD")
 * @param formattedMinAmount Formatted minimum amount string for display below slider (e.g., "6 USD")
 * @param formattedMaxAmount Formatted maximum amount string for display below slider (e.g., "600 USD")
 * @param formattedFiatAmount The current fiat amount value as string (e.g., "500", "6"). Note: decimal
 *        separator and currency are handled internally by the component
 * @param formattedBtcAmount The BTC equivalent amount for display below the fiat input (e.g., "0.0050")
 * @param sliderPosition Current position of the slider as a float (typically 0.0 to 1.0 range)
 * @param onSliderValueChange Callback invoked when the slider value changes
 * @param onTextValueChange Callback invoked when the text input value changes
 * @param minSliderValue Minimum value for the slider range (default: 0f)
 * @param maxSliderValue Maximum value for the slider range (default: 1f)
 * @param isError Whether the input field should show error state (default: false)
 * @param errorMessage Optional error message to display below the input field
 * @param onSliderValueChangeFinish Optional callback invoked when slider interaction finishes
 *
 * Example usage:
 * ```kotlin
 * var textState by remember { mutableStateOf("500") }
 * var sliderState by remember { mutableStateOf(0.5f) }
 *
 * BisqAmountSelectorV0(
 *     modifier = Modifier.padding(16.dp),
 *     quoteCurrencyCode = "USD",
 *     formattedMinAmount = "6 USD",
 *     formattedMaxAmount = "600 USD",
 *     sliderPosition = sliderState,
 *     formattedFiatAmount = textState,
 *     formattedBtcAmount = "0.0050",
 *     onSliderValueChange = { sliderState = it },
 *     onTextValueChange = { textState = it },
 * )
 * ```
 */
@Composable
fun BisqAmountSelector(
    quoteCurrencyCode: String,
    formattedMinAmount: String,
    formattedMaxAmount: String,
    formattedFiatAmount: String,
    formattedBtcAmount: String,
    sliderPosition: Float,
    onSliderValueChange: (sliderValue: Float) -> Unit,
    onTextValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minSliderValue: Float = 0f,
    maxSliderValue: Float = 1f,
    isError: Boolean = false,
    errorMessage: String? = null,
    onSliderValueChangeFinish: (() -> Unit)? = null,
) {
    val decimalSeparator = getDecimalSeparator()
    val formattedFiatAmountValueInt = formattedFiatAmount.substringBefore(decimalSeparator)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqFiatInputField(
            value = formattedFiatAmountValueInt,
            currency = quoteCurrencyCode,
            onValueChange = onTextValueChange,
            isError = isError,
            errorMessage = errorMessage,
        )

        BisqGap.V1()
        BtcSatsText(formattedBtcAmount)

        BisqGap.V3()
        BisqSlider(
            modifier = Modifier.padding(horizontal = 20.dp),
            value = sliderPosition,
            valueRange = minSliderValue..maxSliderValue,
            onValueChange = onSliderValueChange,
            onValueChangeFinished = onSliderValueChangeFinish,
        )

        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
        ) {
            BisqText.SmallRegularGrey("mobile.min".i18n() + " $formattedMinAmount")
            BisqText.SmallRegularGrey("mobile.max".i18n() + " $formattedMaxAmount")
        }
    }
}

@Preview
@Composable
private fun BisqAmountSelectorV0_DefaultPreview() {
    BisqTheme.Preview {
        var textState by remember { mutableStateOf("500") }
        var sliderState by remember { mutableFloatStateOf(0.5f) }

        BisqAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderPosition = sliderState,
            formattedFiatAmount = textState,
            formattedBtcAmount = "0.0050",
            onSliderValueChange = { sliderState = it },
            onTextValueChange = { textState = it },
        )
    }
}

@Preview
@Composable
private fun BisqAmountSelectorV0_MinValuePreview() {
    BisqTheme.Preview {
        var textState by remember { mutableStateOf("6") }
        var sliderState by remember { mutableFloatStateOf(0f) }

        BisqAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderPosition = sliderState,
            formattedFiatAmount = textState,
            formattedBtcAmount = "0.00006",
            onSliderValueChange = { sliderState = it },
            onTextValueChange = { textState = it },
        )
    }
}

@Preview
@Composable
private fun BisqAmountSelectorV0_MaxValuePreview() {
    BisqTheme.Preview {
        var textState by remember { mutableStateOf("540") }
        var sliderState by remember { mutableFloatStateOf(0.9f) }

        BisqAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderPosition = sliderState,
            maxSliderValue = 0.95f,
            formattedFiatAmount = textState,
            formattedBtcAmount = "0.0054",
            onSliderValueChange = { sliderState = it },
            onTextValueChange = { textState = it },
        )
    }
}

@Preview
@Composable
private fun BisqAmountSelectorV0_WithErrorPreview() {
    BisqTheme.Preview {
        var textState by remember { mutableStateOf("9999") }
        var sliderState by remember { mutableFloatStateOf(1.2f) }

        BisqAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderPosition = sliderState,
            formattedFiatAmount = textState,
            formattedBtcAmount = "0.0999",
            onSliderValueChange = { sliderState = it },
            onTextValueChange = { textState = it },
            isError = true,
            errorMessage = "Amount must be between 6 USD and 600 USD",
        )
    }
}
