package network.bisq.mobile.presentation.settings.payment_accounts_musig.model

import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.model.account.getPaymentMethodVOFromCryptoCurrencyCode

data class CryptoAccountVO(
    val accountName: String,
    val currencyName: String,
    val address: String,
    val paymentMethod: PaymentMethodVO,
)

fun CryptoPaymentAccount.toVO(): CryptoAccountVO? =
    when (this) {
        is MoneroAccount ->
            CryptoAccountVO(
                accountName = accountName,
                currencyName = accountPayload.currencyName,
                address = accountPayload.address,
                paymentMethod = PaymentMethodVO.XMR,
            )

        is OtherCryptoAssetAccount ->
            getPaymentMethodVOFromCryptoCurrencyCode(accountPayload.currencyCode)?.let { paymentMethod ->
                CryptoAccountVO(
                    accountName = accountName,
                    currencyName = accountPayload.currencyName,
                    address = accountPayload.address,
                    paymentMethod = paymentMethod,
                )
            }

        else -> null
    }
