package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CreateCashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

fun CreatePaymentAccount.toReviewPaymentAccount(paymentMethod: PaymentMethod): PaymentAccount? =
    when {
        this is CreateAchTransferAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateCashDepositAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateZelleAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateWiseAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateRevolutAccount && paymentMethod is FiatPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateMoneroAccount && paymentMethod is CryptoPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        this is CreateOtherCryptoAssetAccount && paymentMethod is CryptoPaymentMethod -> toReviewPaymentAccount(paymentMethod)
        else -> null
    }
