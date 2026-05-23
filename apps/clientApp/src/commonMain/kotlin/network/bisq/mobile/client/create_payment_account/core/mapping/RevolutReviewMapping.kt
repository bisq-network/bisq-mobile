package network.bisq.mobile.client.create_payment_account.core.mapping

import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.RevolutAccount
import network.bisq.mobile.domain.model.account.fiat.RevolutAccountPayload

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
