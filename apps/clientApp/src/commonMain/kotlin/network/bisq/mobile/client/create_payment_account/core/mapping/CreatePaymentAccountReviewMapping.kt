package network.bisq.mobile.client.create_payment_account.core.mapping

import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod

fun CreatePaymentAccount.toReviewPaymentAccount(paymentMethod: PaymentMethod): PaymentAccount? =
    when {
        this is CreateZelleAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateWiseAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateMoneroAccount && paymentMethod is CryptoPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateOtherCryptoAssetAccount && paymentMethod is CryptoPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        else -> null
    }
