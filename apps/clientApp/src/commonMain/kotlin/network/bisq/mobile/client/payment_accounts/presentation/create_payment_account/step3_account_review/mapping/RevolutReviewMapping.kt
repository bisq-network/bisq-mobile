package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccountPayload

internal fun CreateRevolutAccount.toReviewPaymentAccount(paymentMethod: FiatPaymentMethod): RevolutAccount =
    RevolutAccount(
        accountName = accountName,
        accountPayload =
            RevolutAccountPayload(
                selectedCurrencies = accountPayload.selectedCurrencies,
                userName = accountPayload.userName,
                chargebackRisk = paymentMethod.chargebackRisk,
                paymentMethodName = paymentMethod.name,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
