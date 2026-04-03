package network.bisq.mobile.presentation.settings.payment_accounts_musig.model

import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO

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
            getPaymentMethod()?.let { paymentMethod ->
                CryptoAccountVO(
                    accountName = accountName,
                    currencyName = accountPayload.currencyName,
                    address = accountPayload.address,
                    paymentMethod = paymentMethod,
                )
            }

        else -> null
    }

fun OtherCryptoAssetAccount.getPaymentMethod(): PaymentMethodVO? =
    when (this.accountPayload.currencyCode) {
        "LTC" -> PaymentMethodVO.LTC
        "ETH" -> PaymentMethodVO.ETH
        "BSQ" -> PaymentMethodVO.BSQ
        "ETC" -> PaymentMethodVO.ETC
        "L-BTC" -> PaymentMethodVO.LBTC
        "LN-BTC" -> PaymentMethodVO.LNBTC
        "GRIN" -> PaymentMethodVO.GRIN
        "ZEC" -> PaymentMethodVO.ZEC
        "DOGE" -> PaymentMethodVO.DOGE
        else -> null
    }
