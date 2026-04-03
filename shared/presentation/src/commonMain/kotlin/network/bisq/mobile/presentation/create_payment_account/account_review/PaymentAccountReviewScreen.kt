package network.bisq.mobile.presentation.create_payment_account.account_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun PaymentAccountReviewScreen(
    onCreatePaymentAccount: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review payment account")
        BisqButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreatePaymentAccount,
            text = "Create account",
        )
    }
}
