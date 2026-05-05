package network.bisq.mobile.presentation.settings.payment_accounts_musig.model

import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.model.account.getPaymentTypeVOFromCryptoCurrencyCode

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
