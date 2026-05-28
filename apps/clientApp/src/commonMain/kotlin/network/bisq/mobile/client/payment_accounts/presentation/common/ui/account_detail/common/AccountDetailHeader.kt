package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.ui.PaymentAccountTypeIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun AccountDetailHeader(
    paymentType: PaymentTypeVO?,
    primaryText: String,
    secondaryText: String? = null,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .padding(BisqUIConstants.ScreenPadding)
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        paymentType?.let {
            PaymentAccountTypeIcon(
                paymentType = it,
                size = BisqUIConstants.ScreenPadding2X,
            )
        }
        Column {
            BisqText.BaseRegular(primaryText)
            secondaryText
                ?.takeIf { it.isNotBlank() }
                ?.let { BisqText.BaseRegularGrey(it) }
        }
    }
}

@Preview
@Composable
private fun AccountDetailHeaderWithIconAndSecondaryTextPreview() {
    BisqTheme.Preview {
        AccountDetailHeader(
            paymentType = PaymentTypeVO.XMR,
            primaryText = "XMR",
            secondaryText = "Monero",
        )
    }
}

@Preview
@Composable
private fun AccountDetailHeaderWithIconOnlyPreview() {
    BisqTheme.Preview {
        AccountDetailHeader(
            paymentType = PaymentTypeVO.WISE,
            primaryText = "Wise",
        )
    }
}

@Preview
@Composable
private fun AccountDetailHeaderWithoutIconPreview() {
    BisqTheme.Preview {
        AccountDetailHeader(
            paymentType = null,
            primaryText = "ETH",
            secondaryText = "Ethereum",
        )
    }
}
