package network.bisq.mobile.presentation.create_payment_account.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun UnsupportedAccountState(modifier: Modifier = Modifier) {
    BisqText.BaseRegular(
        text = "mobile.user.paymentAccounts.unsupported".i18n(),
        color = BisqTheme.colors.warning,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun UnsupportedAccountStatePreview() {
    BisqTheme.Preview {
        UnsupportedAccountState()
    }
}
