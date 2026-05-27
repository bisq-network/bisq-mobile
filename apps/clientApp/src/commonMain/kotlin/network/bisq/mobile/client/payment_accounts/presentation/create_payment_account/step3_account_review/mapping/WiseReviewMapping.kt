package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.WiseAccount
import network.bisq.mobile.domain.model.account.fiat.WiseAccountPayload

internal fun CreateWiseAccount.toReviewPaymentAccount(paymentMethod: FiatPaymentMethod): WiseAccount =
    WiseAccount(
        accountName = accountName,
        accountPayload =
            WiseAccountPayload(
                selectedCurrencies = accountPayload.selectedCurrencies,
                holderName = accountPayload.holderName,
                email = accountPayload.email,
                chargebackRisk = paymentMethod.chargebackRisk,
                paymentMethodName = paymentMethod.name,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
