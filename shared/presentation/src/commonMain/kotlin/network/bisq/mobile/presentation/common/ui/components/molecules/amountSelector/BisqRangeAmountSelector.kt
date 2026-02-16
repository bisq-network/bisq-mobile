package network.bisq.mobile.presentation.common.ui.components.molecules.amountSelector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import network.bisq.mobile.presentation.common.ui.components.atoms.slider.BisqRangeSlider
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqFiatInputField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A composable that allows users to select a range of amounts (min and max) using two text input
 * fields and a range slider, displaying both fiat and BTC values for each bound.
 *
 * This component combines two [BisqFiatInputField] instances for direct text input of min/max values,
 * [BtcSatsText] components showing BTC equivalents, and a [BisqRangeSlider] for visual range selection.
 *
 * @param modifier Modifier to be applied to the root container
 * @param quoteCurrencyCode The currency code to display as suffix in input fields (e.g., "USD")
 * @param formattedMinAmount Formatted minimum amount string for display below slider (e.g., "6 USD")
 * @param formattedMaxAmount Formatted maximum amount string for display below slider (e.g., "600 USD")
 * @param formattedQuoteSideMinRangeAmount The current min fiat amount value as string (e.g., "6")
 * @param formattedQuoteSideMaxRangeAmount The current max fiat amount value as string (e.g., "450")
 * @param formattedBaseSideMinRangeAmount The BTC equivalent for min amount display (e.g., "0.00006")
 * @param formattedBaseSideMaxRangeAmount The BTC equivalent for max amount display (e.g., "0.0054")
 * @param sliderRange Current range of the slider as ClosedFloatingPointRange (e.g., 0.25f..0.75f)
 * @param onSliderRangeChange Callback invoked when the slider range changes
 * @param onMinAmountTextValueChange Callback invoked when the min text input value changes
 * @param onMaxAmountTextValueChange Callback invoked when the max text input value changes
 * @param sliderValueRange Total value range for the slider (default: 0f..1f)
 * @param isMinError Whether the min input field should show error state (default: false)
 * @param isMaxError Whether the max input field should show error state (default: false)
 * @param minErrorMessage Optional error message for the min input field
 * @param maxErrorMessage Optional error message for the max input field
 * @param onSliderRangeChangeFinish Optional callback invoked when slider interaction finishes
 *
 * Example usage:
 * ```kotlin
 * var minTextState by remember { mutableStateOf("6") }
 * var maxTextState by remember { mutableStateOf("450") }
 * var sliderRange by remember { mutableStateOf(0.25f..0.75f) }
 *
 * BisqRangeAmountSelector(
 *     modifier = Modifier.padding(16.dp),
 *     quoteCurrencyCode = "USD",
 *     formattedMinAmount = "6 USD",
 *     formattedMaxAmount = "600 USD",
 *     sliderRange = sliderRange,
 *     formattedQuoteSideMinRangeAmount = minTextState,
 *     formattedQuoteSideMaxRangeAmount = maxTextState,
 *     formattedBaseSideMinRangeAmount = "0.00006",
 *     formattedBaseSideMaxRangeAmount = "0.0054",
 *     onSliderRangeChange = { sliderRange = it },
 *     onMinAmountTextValueChange = { minTextState = it },
 *     onMaxAmountTextValueChange = { maxTextState = it },
 * )
 * ```
 */
@Composable
fun BisqRangeAmountSelector(
    quoteCurrencyCode: String,
    formattedMinAmount: String,
    formattedMaxAmount: String,
    formattedQuoteSideMinRangeAmount: String,
    formattedQuoteSideMaxRangeAmount: String,
    formattedBaseSideMinRangeAmount: String,
    formattedBaseSideMaxRangeAmount: String,
    sliderRange: ClosedFloatingPointRange<Float>,
    onSliderRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onMinAmountTextValueChange: (String) -> Unit,
    onMaxAmountTextValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    sliderValueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    isMinError: Boolean = false,
    isMaxError: Boolean = false,
    minErrorMessage: String? = null,
    maxErrorMessage: String? = null,
    onSliderRangeChangeFinish: (() -> Unit)? = null,
) {
    val decimalSeparator = getDecimalSeparator()
    val minAmountWithoutDecimal = formattedQuoteSideMinRangeAmount.substringBefore(decimalSeparator)
    val maxAmountWithoutDecimal = formattedQuoteSideMaxRangeAmount.substringBefore(decimalSeparator)

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
            ) {
                BisqText.SmallRegularGrey("mobile.min".i18n())
                BisqFiatInputField(
                    value = minAmountWithoutDecimal,
                    currency = quoteCurrencyCode,
                    onValueChange = onMinAmountTextValueChange,
                    isError = isMinError,
                    errorMessage = minErrorMessage,
                    smallFont = true,
                )
                BtcSatsText(formattedBaseSideMinRangeAmount)
            }

            BisqGap.H1()

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
            ) {
                BisqText.SmallRegularGrey("mobile.max".i18n())
                BisqFiatInputField(
                    value = maxAmountWithoutDecimal,
                    currency = quoteCurrencyCode,
                    onValueChange = onMaxAmountTextValueChange,
                    isError = isMaxError,
                    errorMessage = maxErrorMessage,
                    smallFont = true,
                )
                BtcSatsText(formattedBaseSideMaxRangeAmount)
            }
        }

        BisqGap.V3()

        BisqRangeSlider(
            modifier = Modifier.padding(horizontal = 20.dp),
            value = sliderRange,
            onValueChange = onSliderRangeChange,
            valueRange = sliderValueRange,
            onValueChangeFinished = onSliderRangeChangeFinish,
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
private fun BisqRangeAmountSelector_DefaultPreview() {
    BisqTheme.Preview {
        var minTextState by remember { mutableStateOf("150") }
        var maxTextState by remember { mutableStateOf("450") }
        var sliderRange by remember { mutableStateOf(0.25f..0.75f) }

        BisqRangeAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderRange = sliderRange,
            formattedQuoteSideMinRangeAmount = minTextState,
            formattedQuoteSideMaxRangeAmount = maxTextState,
            formattedBaseSideMinRangeAmount = "0.00006",
            formattedBaseSideMaxRangeAmount = "0.0054",
            onSliderRangeChange = { sliderRange = it },
            onMinAmountTextValueChange = { minTextState = it },
            onMaxAmountTextValueChange = { maxTextState = it },
        )
    }
}

@Preview
@Composable
private fun BisqRangeAmountSelector_MinRangePreview() {
    BisqTheme.Preview {
        var minTextState by remember { mutableStateOf("6") }
        var maxTextState by remember { mutableStateOf("60") }
        var sliderRange by remember { mutableStateOf(0f..0.1f) }

        BisqRangeAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderRange = sliderRange,
            formattedQuoteSideMinRangeAmount = minTextState,
            formattedQuoteSideMaxRangeAmount = maxTextState,
            formattedBaseSideMinRangeAmount = "0.00006",
            formattedBaseSideMaxRangeAmount = "0.0006",
            onSliderRangeChange = { sliderRange = it },
            onMinAmountTextValueChange = { minTextState = it },
            onMaxAmountTextValueChange = { maxTextState = it },
        )
    }
}

@Preview
@Composable
private fun BisqRangeAmountSelector_MaxRangePreview() {
    BisqTheme.Preview {
        var minTextState by remember { mutableStateOf("540") }
        var maxTextState by remember { mutableStateOf("600") }
        var sliderRange by remember { mutableStateOf(0.9f..1f) }

        BisqRangeAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderRange = sliderRange,
            sliderValueRange = 0f..1f,
            formattedQuoteSideMinRangeAmount = minTextState,
            formattedQuoteSideMaxRangeAmount = maxTextState,
            formattedBaseSideMinRangeAmount = "0.0054",
            formattedBaseSideMaxRangeAmount = "0.0060",
            onSliderRangeChange = { sliderRange = it },
            onMinAmountTextValueChange = { minTextState = it },
            onMaxAmountTextValueChange = { maxTextState = it },
        )
    }
}

@Preview
@Composable
private fun BisqRangeAmountSelector_WithErrorPreview() {
    BisqTheme.Preview {
        var minTextState by remember { mutableStateOf("9999") }
        var maxTextState by remember { mutableStateOf("99999") }
        var sliderRange by remember { mutableStateOf(1.2f..1.5f) }

        BisqRangeAmountSelector(
            modifier = Modifier.padding(16.dp),
            quoteCurrencyCode = "USD",
            formattedMinAmount = "6 USD",
            formattedMaxAmount = "600 USD",
            sliderRange = sliderRange,
            sliderValueRange = 0f..2f,
            formattedQuoteSideMinRangeAmount = minTextState,
            formattedQuoteSideMaxRangeAmount = maxTextState,
            formattedBaseSideMinRangeAmount = "0.0999",
            formattedBaseSideMaxRangeAmount = "0.9999",
            onSliderRangeChange = { sliderRange = it },
            onMinAmountTextValueChange = { minTextState = it },
            onMaxAmountTextValueChange = { maxTextState = it },
            isMinError = true,
            isMaxError = true,
            minErrorMessage = "Min amount exceeds maximum",
            maxErrorMessage = "Max amount exceeds maximum",
        )
    }
}
