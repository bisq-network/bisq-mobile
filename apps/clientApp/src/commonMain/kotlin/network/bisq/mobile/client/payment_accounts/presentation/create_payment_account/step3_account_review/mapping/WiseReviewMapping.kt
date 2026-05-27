package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccountPayload

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
