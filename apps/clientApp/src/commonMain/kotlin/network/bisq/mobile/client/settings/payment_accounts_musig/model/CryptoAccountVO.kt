package network.bisq.mobile.client.settings.payment_accounts_musig.model

import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.presentation.model.account.getPaymentTypeVOFromCryptoCurrencyCode
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount

data class CryptoAccountVO(
    val accountName: String,
    val currencyName: String,
    val address: String,
    val paymentType: PaymentTypeVO,
)

fun CryptoPaymentAccount.toVO(): CryptoAccountVO? =
    when (this) {
        is MoneroAccount ->
            CryptoAccountVO(
                accountName = accountName,
                currencyName = "${accountPayload.currencyName} (${accountPayload.currencyCode})",
                address = accountPayload.address,
                paymentType = PaymentTypeVO.XMR,
            )

        is OtherCryptoAssetAccount ->
            getPaymentTypeVOFromCryptoCurrencyCode(accountPayload.currencyCode)?.let { paymentMethod ->
                CryptoAccountVO(
                    accountName = accountName,
                    currencyName = "${accountPayload.currencyName} (${accountPayload.currencyCode})",
                    address = accountPayload.address,
                    paymentType = paymentMethod,
                )
            }

        else -> null
    }
