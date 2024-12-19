package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.helpers.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqAmountSelector(
    minAmount: Double,
    maxAmount: Double,
    exchangeRate: Double,
    currency: String,
    onValueChange: ((Double) -> Unit)? = null
) {
    var fiatValue by remember { mutableDoubleStateOf((minAmount + maxAmount) * 0.5) }
    val sats = (100_000_000L * (fiatValue.toDouble()) / exchangeRate).toLong()

    LaunchedEffect(fiatValue) {
        onValueChange?.invoke(fiatValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        FiatInputField(
            text = fiatValue.toInt().toString(),
            onValueChanged = { fiatValue = it.toDoubleOrNull() ?: (0.0) },
            currency = currency
        )

        if (fiatValue < minAmount || fiatValue > maxAmount) {
            BisqText.baseRegular("Amount out of range", color = BisqTheme.colors.danger)
        }

        BtcSatsText(sats)

        Column {
            BisqSlider(
                fiatValue.toFloat(),
                onValueChange = { fiatValue = it.toDouble() },
                minAmount.toFloat(),
                maxAmount.toFloat(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            ) {
                BisqText.smallRegular(
                    text = "Min ${minAmount.toStringWith2Decimals()} $currency",
                    color = BisqTheme.colors.grey2
                )
                BisqText.smallRegular(
                    text = "Max ${maxAmount.toStringWith2Decimals()} $currency",
                    color = BisqTheme.colors.grey2
                )
            }
        }
    }
}
