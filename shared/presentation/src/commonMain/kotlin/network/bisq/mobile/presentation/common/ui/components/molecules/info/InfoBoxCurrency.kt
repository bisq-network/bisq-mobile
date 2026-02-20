package network.bisq.mobile.presentation.common.ui.components.molecules.info

import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.common.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun InfoBoxCurrency(
    label: String,
    value: String,
    rightAlign: Boolean = false,
) {
    InfoBox(
        label = label,
        rightAlign = rightAlign,
        valueComposable = {
            AmountWithCurrency(value)
        },
    )
}

@Preview
@Composable
private fun InfoBoxCurrencyPreview() {
    BisqTheme.Preview {
        InfoBoxCurrency(
            label = "AMOUNT TO PAY",
            value = "1,000.00 USD",
        )
    }
}

@Preview
@Composable
private fun InfoBoxCurrency_RightAlignPreview() {
    BisqTheme.Preview {
        InfoBoxCurrency(
            label = "AMOUNT TO RECEIVE",
            value = "60.00 USD",
            rightAlign = true,
        )
    }
}
