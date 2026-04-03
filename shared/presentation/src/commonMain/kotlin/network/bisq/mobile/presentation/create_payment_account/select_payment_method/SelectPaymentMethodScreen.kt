package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun SelectPaymentMethodScreen(
    accountType: PaymentAccountType,
    onContinue: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BisqText.SmallRegular("Select ${accountType.name.lowercase()} payment method")
        BisqButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinue,
            text = "Continue",
        )
    }
}
