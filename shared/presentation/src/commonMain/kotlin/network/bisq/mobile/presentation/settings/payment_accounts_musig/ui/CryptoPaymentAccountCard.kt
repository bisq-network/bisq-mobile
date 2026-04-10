package network.bisq.mobile.presentation.settings.payment_accounts_musig.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.CryptoAccountVO

@Composable
fun CryptoPaymentAccountCard(
    account: CryptoAccountVO,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding)
                .fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PaymentAccountMethodIcon(
                paymentMethod = account.paymentMethod,
            )
            Column(modifier = Modifier.weight(1f)) {
                BisqText.H4Regular(account.accountName)
                BisqGap.VQuarter()
                BisqText.BaseRegular(
                    text = account.currencyName,
                    color = BisqTheme.colors.mid_grey20,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BisqText.StyledText(
            text = account.address,
            style = BisqTheme.typography.h6Light,
            color = BisqTheme.colors.mid_grey30,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@ExcludeFromCoverage
private fun previewCryptoAccount(
    accountName: String = "Main Monero Wallet",
    currencyName: String = "Monero",
    address: String = "44AFFq5kSiGBoZ",
    paymentMethod: PaymentMethodVO = PaymentMethodVO.XMR,
): CryptoAccountVO =
    CryptoAccountVO(
        accountName = accountName,
        currencyName = currencyName,
        address = address,
        paymentMethod = paymentMethod,
    )

@Preview
@Composable
private fun CryptoPaymentAccountCardPreview_MoneroPreview() {
    BisqTheme.Preview {
        CryptoPaymentAccountCard(
            account = previewCryptoAccount(),
        )
    }
}

@Preview
@Composable
private fun CryptoPaymentAccountCardPreview_LitecoinLongNamePreview() {
    BisqTheme.Preview {
        CryptoPaymentAccountCard(
            account =
                previewCryptoAccount(
                    accountName = "Cold Storage Savings Wallet",
                    currencyName = "Litecoin",
                    address = "ltc1qexampleaddress",
                    paymentMethod = PaymentMethodVO.LTC,
                ),
        )
    }
}
