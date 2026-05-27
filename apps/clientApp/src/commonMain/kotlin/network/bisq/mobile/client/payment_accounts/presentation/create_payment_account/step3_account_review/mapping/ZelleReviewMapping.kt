package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload

internal fun CreateZelleAccount.toReviewPaymentAccount(paymentMethod: FiatPaymentMethod): ZelleAccount =
    ZelleAccount(
        accountName = accountName,
        accountPayload =
            ZelleAccountPayload(
                holderName = accountPayload.holderName,
                emailOrMobileNr = accountPayload.emailOrMobileNr,
                chargebackRisk = paymentMethod.chargebackRisk,
                paymentMethodName = paymentMethod.name,
                currency = paymentMethod.supportedCurrencies.first(),
                country = paymentMethod.supportedCountries.first(),
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
