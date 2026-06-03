package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun ColumnScope.AccountDetailDetailsSection(
    creationDate: String?,
    tradeLimitInfo: String?,
    tradeDuration: String?,
) {
    if (creationDate.isNullOrBlank() && tradeLimitInfo.isNullOrBlank() && tradeDuration.isNullOrBlank()) {
        return
    }

    BisqGap.VHalf()
    AccountDetailSectionHeader("paymentAccounts.details".i18n())

    creationDate
        ?.takeIf { it.isNotBlank() }
        ?.let {
            AccountDetailFieldRow(
                label = "paymentAccounts.accountCreationDate".i18n(),
                value = it,
            )
        }

    tradeLimitInfo
        ?.takeIf { it.isNotBlank() }
        ?.let {
            AccountDetailFieldRow(
                label = "paymentAccounts.tradeLimit".i18n(),
                value = it,
            )
        }

    tradeDuration
        ?.takeIf { it.isNotBlank() }
        ?.let {
            AccountDetailFieldRow(
                label = "paymentAccounts.tradeDuration".i18n(),
                value = it,
            )
        }
}

@Composable
private fun AccountDetailSectionHeader(label: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqText.SmallLight(
            text = label.uppercase(),
            color = BisqTheme.colors.mid_grey20,
        )
        BisqGap.VHalf()
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            color = BisqTheme.colors.mid_grey10,
            content = {},
        )
    }
}

@Preview
@Composable
private fun AccountDetailDetailsSectionPreview() {
    BisqTheme.Preview {
        Column(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            AccountDetailDetailsSection(
                creationDate = "2026-06-03",
                tradeLimitInfo = "0.25 BTC",
                tradeDuration = "1 day",
            )
        }
    }
}
