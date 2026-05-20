package network.bisq.mobile.client.create_payment_account.core.mapping

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
                currency = paymentMethod.supportedCurrencies.joinToString(", ") { it.code },
                country = paymentMethod.supportedCountries.joinToString(", ") { it.name },
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
