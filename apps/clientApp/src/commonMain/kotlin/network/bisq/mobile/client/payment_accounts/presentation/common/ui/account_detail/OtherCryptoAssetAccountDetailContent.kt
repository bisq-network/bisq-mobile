package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.client.common.presentation.model.account.getPaymentTypeVOFromCryptoCurrencyCode
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.CryptoAccountDetailHeader
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.CryptoAutoConfDetailRows
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.CryptoCommonDetailRows
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccountPayload
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun OtherCryptoAssetAccountDetailContent(
    account: OtherCryptoAssetAccount,
) {
    val payload = account.accountPayload
    val paymentType = remember { getPaymentTypeVOFromCryptoCurrencyCode(payload.currencyCode) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            CryptoAccountDetailHeader(
                paymentType = paymentType,
                currencyName = payload.currencyName,
                currencyCode = payload.currencyCode,
            )

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                CryptoCommonDetailRows(
                    address = payload.address,
                    isInstant = payload.isInstant,
                    showAddress = true,
                )

                if (payload.supportAutoConf) {
                    CryptoAutoConfDetailRows(
                        isAutoConf = payload.isAutoConf == true,
                        autoConfNumConfirmations = payload.autoConfNumConfirmations,
                        autoConfMaxTradeAmount = payload.autoConfMaxTradeAmount,
                        autoConfExplorerUrls = payload.autoConfExplorerUrls,
                    )
                }

                AccountDetailDetailsSection(
                    creationDate = account.creationDate,
                    tradeLimitInfo = account.tradeLimitInfo,
                    tradeDuration = account.tradeDuration,
                )
            }
        }
    }
}

private val previewAccount =
    OtherCryptoAssetAccount(
        accountName = "My Ethereum Account",
        accountPayload =
            OtherCryptoAssetAccountPayload(
                address = "0x1234567890abcdef1234567890abcdef12345678",
                isInstant = true,
                isAutoConf = true,
                autoConfNumConfirmations = 2,
                autoConfMaxTradeAmount = 1,
                autoConfExplorerUrls = "https://explorer.example.com",
                currencyCode = "ETH",
                currencyName = "Ethereum",
                supportAutoConf = true,
            ),
        creationDate = null,
        tradeLimitInfo = null,
        tradeDuration = null,
    )

@Preview
@Composable
private fun OtherCryptoAssetAccountDetailContentPreview() {
    BisqTheme.Preview {
        OtherCryptoAssetAccountDetailContent(
            account = previewAccount,
        )
    }
}
